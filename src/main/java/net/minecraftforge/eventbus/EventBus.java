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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.jodah.typetools.TypeResolver;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventListenerHelper;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.GenericEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.IEventExceptionHandler;
import net.minecraftforge.eventbus.api.IEventListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class EventBus implements IEventExceptionHandler, IEventBus {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Marker EVENTBUS = MarkerManager.getMarker("EVENTBUS");

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

		if (handler == null) {
			exceptionHandler = this;
		} else {
			exceptionHandler = handler;
		}

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

		// TODO: Track the added events for a later unregister() call
		registrar.accept(this);
	}

	@SuppressWarnings("unchecked")
	private void registerObject(final Class<?> clazz, final Object obj, final boolean required) {
		//noinspection RedundantCast
		final BiConsumer<Object, IEventBus> registrar = (BiConsumer<Object, IEventBus>) EventRegistrarRegistryImpl.INSTANCE.getInstanceRegistrar(clazz);

		if (registrar == null) {
			if (required) {
				// TODO: This doesn't handle the case of an event with no @SubscribeEvent annotations,
				//  or where the registrar has not yet been registered.

				System.err.println("Missing instance event registrar for " + clazz);
			}

			return;
		}

		// TODO: Track the added events for a later unregister() call
		registrar.accept(obj, this);

		Class<?> superclass = clazz.getSuperclass();

		if (superclass != null) {
			registerObject(superclass, obj, false);
		}

		for (Class<?> currInterface : clazz.getInterfaces()) {
			registerObject(currInterface, obj, false);
		}
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
			LOGGER.error(EVENTBUS, "Failed to resolve handler for \"{}\"", consumer);
			throw new IllegalStateException("Failed to resolve consumer event type: " + consumer);
		}

		if (Objects.equals(eventClass, Event.class)) {
			LOGGER.warn(EVENTBUS, "Attempting to add a Lambda listener with computed generic type of Event. "
					+ "Are you sure this is what you meant? NOTE : there are complex lambda forms where "
					+ "the generic type information is erased and cannot be recovered at runtime.");
		}

		addListener(priority, filter, eventClass, consumer);
	}

	private <T extends Event> void addListener(final EventPriority priority, final Predicate<? super T> filter, final Class<T> eventClass, final Consumer<T> consumer) {
		addListener(priority, filter, eventClass, consumer, consumer);
	}

	private <T extends Event> void addListener(final EventPriority priority, final Predicate<? super T> filter, final Class<T> eventClass, final Consumer<T> consumer, final Object context) {
		IEventListener listener = event -> doCastFilter(filter, consumer, event);

		ListenerList listenerList = EventListenerHelper.getListenerList(eventClass);
		listenerList.register(busID, priority, listener);

		List<IEventListener> others = listeners.computeIfAbsent(context, k -> Collections.synchronizedList(new ArrayList<>()));
		others.add(listener);
	}

	@SuppressWarnings("unchecked")
	private <T extends Event> void doCastFilter(final Predicate<? super T> filter, final Consumer<T> consumer, final Event e) {
		T cast = (T) e;

		if (filter.test(cast)) {
			consumer.accept(cast);
		}
	}

	@Override
	public void unregister(Object object) {
		List<IEventListener> list = listeners.remove(object);

		if (list == null) {
			// ie, registered with registerObject / registerClass
			// this message assumes that classes that implement an unrelated consumer interface and are also event listeners about to be unregistered don't exist.
			if (!Consumer.class.isAssignableFrom(object.getClass())) {
				LOGGER.warn("Attempted to unregister {} (class {}) from the EventBus, unregistering non-consumer Objects isn't properly supported currently", object, object.getClass());
			}

			return;
		}

		for (IEventListener listener : list) {
			ListenerList.unregisterAll(busID, listener);
		}
	}

	@Override
	public boolean post(Event event) {
		if (shutdown) {
			return false;
		}

		IEventListener[] listeners = event.getListenerList().getListeners(busID);
		int index = 0;

		try {
			for (; index < listeners.length; index++) {
				if (!trackPhases && Objects.equals(listeners[index].getClass(), EventPriority.class)) {
					continue;
				}

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
