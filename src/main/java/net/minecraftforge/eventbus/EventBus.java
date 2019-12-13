/*
 * Minecraft Forge
 * Copyright (c) 2016.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.eventbus;

import net.jodah.typetools.TypeResolver;
import net.minecraftforge.eventbus.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static net.minecraftforge.eventbus.LogMarkers.EVENTBUS;

public class EventBus implements IEventExceptionHandler, IEventBus {
	private static final Logger LOGGER = LogManager.getLogger();
	private static AtomicInteger maxID = new AtomicInteger(0);
	private final boolean trackPhases;
	private final int busID = maxID.getAndIncrement();
	private final IEventExceptionHandler exceptionHandler;
	private ConcurrentHashMap<Object, List<IEventListener>> listeners = new ConcurrentHashMap<>();
	private volatile boolean shutdown = false;

	private EventBus() {
		ListenerList.resize(busID + 1);
		exceptionHandler = this;
		this.trackPhases = true;
	}

	private EventBus(final IEventExceptionHandler handler, boolean trackPhase, boolean startShutdown) {
		ListenerList.resize(busID + 1);
		if (handler == null) exceptionHandler = this;
		else exceptionHandler = handler;
		this.trackPhases = trackPhase;
		this.shutdown = startShutdown;
	}

	public EventBus(final BusBuilder busBuilder) {
		this(busBuilder.getExceptionHandler(), busBuilder.getTrackPhases(), busBuilder.isStartingShutdown());
	}

	private void registerClass(final Class<?> clazz) {
		final Consumer<IEventBus> registrar = EventRegistrarRegistryImpl.INSTANCE.getStaticRegistrar(clazz);

		if (registrar == null) {
			// TODO: This doesn't handle the case of an event with no @SubscribeEvent annotations,
			//  or where the registrar has not yet been registered.

			System.err.println("Missing static event registrar for " + clazz);

			return;
		}

		registrar.accept(this);
	}

	@SuppressWarnings("unchecked")
	private void registerObject(final Class<?> clazz, final Object obj, final boolean required) {
		final BiConsumer<Object, IEventBus> registrar = (BiConsumer<Object, IEventBus>)EventRegistrarRegistryImpl.INSTANCE.getInstanceRegistrar(clazz);

		if(registrar == null) {
			if(required) {
				// TODO: This doesn't handle the case of an event with no @SubscribeEvent annotations,
				//  or where the registrar has not yet been registered.

				System.err.println("Missing instance event registrar for " + clazz);
			}

			return;
		}

		registrar.accept(obj, this);

		final HashSet<Class<?>> classes = new HashSet<>();
		typesFor(clazz, classes);

		classes.remove(clazz);

		for(Class<?> subclazz: classes) {
			// TODO: Cases where both a subclass and a superclass have an annotation on a method are not handled here!

			registerObject(subclazz, obj, false);
		}
	}


	private void typesFor(final Class<?> clz, final Set<Class<?>> visited) {
		if (clz.getSuperclass() == null) {
			return;
		}

		typesFor(clz.getSuperclass(), visited);
		Arrays.stream(clz.getInterfaces()).forEach(i -> typesFor(i, visited));

		visited.add(clz);
	}

	@Override
	public void register(final Object target) {
		if (listeners.containsKey(target)) {
			return;
		}

		if (target.getClass() == Class.class) {
			registerClass((Class<?>) target);
		} else {
			registerObject(target.getClass(), target, true);
		}
	}

	private <T extends Event> Predicate<T> passCancelled(final boolean ignored) {
		return e -> ignored || !e.isCancelable() || !e.isCanceled();
	}

	private <T extends GenericEvent<? extends F>, F> Predicate<T> passGenericFilter(Class<F> type) {
		return e -> e.getGenericType() == type;
	}

	@Override
	public <T extends Event> void addListener(final Consumer<T> consumer) {
		addListener(EventPriority.NORMAL, consumer);
	}

	@Override
	public <T extends Event> void addListener(final EventPriority priority, final Consumer<T> consumer) {
		addListener(priority, false, consumer);
	}

	@Override
	public <T extends Event> void addListener(final EventPriority priority, final boolean receiveCancelled, final Consumer<T> consumer) {
		addListener(priority, passCancelled(receiveCancelled), consumer);
	}

	@Override
	public <T extends Event> void addListener(final EventPriority priority, final boolean receiveCancelled, final Class<T> eventType, final Consumer<T> consumer) {
		addListener(priority, passCancelled(receiveCancelled), eventType, consumer);
	}

	@Override
	public <T extends GenericEvent<? extends F>, F> void addGenericListener(final Class<F> genericClassFilter, final Consumer<T> consumer) {
		addGenericListener(genericClassFilter, EventPriority.NORMAL, consumer);
	}

	@Override
	public <T extends GenericEvent<? extends F>, F> void addGenericListener(final Class<F> genericClassFilter, final EventPriority priority, final Consumer<T> consumer) {
		addGenericListener(genericClassFilter, priority, false, consumer);
	}

	@Override
	public <T extends GenericEvent<? extends F>, F> void addGenericListener(final Class<F> genericClassFilter, final EventPriority priority, final boolean receiveCancelled, final Consumer<T> consumer) {
		addListener(priority, passGenericFilter(genericClassFilter).and(passCancelled(receiveCancelled)), consumer);
	}

	@Override
	public <T extends GenericEvent<? extends F>, F> void addGenericListener(final Class<F> genericClassFilter, final EventPriority priority, final boolean receiveCancelled, final Class<T> eventType, final Consumer<T> consumer) {
		addListener(priority, passGenericFilter(genericClassFilter).and(passCancelled(receiveCancelled)), eventType, consumer);
	}

	@SuppressWarnings("unchecked")
	private <T extends Event> void addListener(final EventPriority priority, final Predicate<? super T> filter, final Consumer<T> consumer) {
		final Class<T> eventClass = (Class<T>) TypeResolver.resolveRawArgument(Consumer.class, consumer.getClass());
		if ((Class<?>) eventClass == TypeResolver.Unknown.class) {
			LOGGER.error(EVENTBUS, "Failed to resolve handler for \"{}\"", consumer.toString());
			throw new IllegalStateException("Failed to resolve consumer event type: " + consumer.toString());
		}
		if (Objects.equals(eventClass, Event.class))
			LOGGER.warn(EVENTBUS, "Attempting to add a Lambda listener with computed generic type of Event. " +
					"Are you sure this is what you meant? NOTE : there are complex lambda forms where " +
					"the generic type information is erased and cannot be recovered at runtime.");
		addListener(priority, filter, eventClass, consumer);
	}

	private <T extends Event> void addListener(final EventPriority priority, final Predicate<? super T> filter, final Class<T> eventClass, final Consumer<T> consumer) {
		addToListeners(consumer, eventClass, e -> doCastFilter(filter, eventClass, consumer, e), priority);
	}

	@SuppressWarnings("unchecked")
	private <T extends Event> void doCastFilter(final Predicate<? super T> filter, final Class<T> eventClass, final Consumer<T> consumer, final Event e) {
		T cast = (T) e;
		if (filter.test(cast)) {
			consumer.accept(cast);
		}
	}

	private void addToListeners(final Object target, final Class<?> eventType, final IEventListener listener, final EventPriority priority) {
		ListenerList listenerList = EventListenerHelper.getListenerList(eventType);
		listenerList.register(busID, priority, listener);
		List<IEventListener> others = listeners.computeIfAbsent(target, k -> Collections.synchronizedList(new ArrayList<>()));
		others.add(listener);
	}

	@Override
	public void unregister(Object object) {
		List<IEventListener> list = listeners.remove(object);
		if (list == null)
			return;
		for (IEventListener listener : list) {
			ListenerList.unregisterAll(busID, listener);
		}
	}

	@Override
	public boolean post(Event event) {
		if (shutdown) return false;

		IEventListener[] listeners = event.getListenerList().getListeners(busID);
		int index = 0;
		try {
			for (; index < listeners.length; index++) {
				if (!trackPhases && Objects.equals(listeners[index].getClass(), EventPriority.class)) continue;
				listeners[index].invoke(event);
			}
		} catch (Throwable throwable) {
			exceptionHandler.handleException(this, event, listeners, index, throwable);
			throw throwable;
		}
		return event.isCancelable() && event.isCanceled();
	}

	@Override
	public void handleException(IEventBus bus, Event event, IEventListener[] listeners, int index, Throwable throwable) {
		LOGGER.error(EVENTBUS, () -> new EventBusErrorMessage(event, index, listeners, throwable));
	}

	@Override
	public void shutdown() {
		LOGGER.fatal(EVENTBUS, "EventBus {} shutting down - future events will not be posted.", busID, new Exception("stacktrace"));
		this.shutdown = true;
	}

	@Override
	public void start() {
		this.shutdown = false;
	}
}
