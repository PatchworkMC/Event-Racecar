package net.minecraftforge.eventbus.api;

import java.util.function.Consumer;

/**
 * EventBus API.
 *
 * <p>Register for events and post events.
 *
 * <p>To construct event bus instances, it is recommended to use {@link BusBuilder}.
 */
public interface IEventBus {
	/**
	 * Register an instance object or a Class, and add listeners for using an already registered event registrar.
	 *
	 * <p>Depending on what is passed as an argument, different listener creation behaviour is performed.
	 *
	 * <dl>
	 *     <dt>Object Instance</dt>
	 *     <dd>The event registrar given by {@link EventRegistrarRegistry#getInstanceRegistrar(Class)} is invoked with
	 *     an instance of this event bus.</dd>
	 *     <dt>Class Instance</dt>
	 *     <dd>The event registrar given by {@link EventRegistrarRegistry#getStaticRegistrar(Class)} is invoked with
	 *     an instance of this event bus.</dd>
	 * </dl>
	 *
	 * @param target Either a {@link Class} instance or an arbitrary object, for scanning and event listener creation
	 */
	void register(Object target);

	/**
	 * Add a consumer listener with default {@link EventPriority#NORMAL} and not receiving cancelled events.
	 *
	 * @param consumer Callback to invoke when a matching event is received
	 * @param <T>      The {@link Event} subclass to listen for
	 */
	<T extends Event> void addListener(Consumer<T> consumer);

	<T extends Event> void addExplicitListener(Consumer<T> consumer, Class<T> eventClass);

	/**
	 * Add a consumer listener with the specified {@link EventPriority} and not receiving cancelled events.
	 *
	 * @param priority {@link EventPriority} for this listener
	 * @param consumer Callback to invoke when a matching event is received
	 * @param <T>      The {@link Event} subclass to listen for
	 */
	<T extends Event> void addListener(EventPriority priority, Consumer<T> consumer);

	<T extends Event> void addExplicitListener(EventPriority priority, Consumer<T> consumer, Class<T> eventClass);

	/**
	 * Add a consumer listener with the specified {@link EventPriority} and potentially cancelled events.
	 *
	 * @param priority         {@link EventPriority} for this listener
	 * @param receiveCancelled Indicate if this listener should receive events that have been {@link Event#isCanceled() cancelled}
	 * @param consumer         Callback to invoke when a matching event is received
	 * @param <T>              The {@link Event} subclass to listen for
	 */
	<T extends Event> void addListener(EventPriority priority, boolean receiveCancelled, Consumer<T> consumer);

	/**
	 * Add a consumer listener with the specified {@link EventPriority} and potentially cancelled events.
	 *
	 * <p>Use this method when one of the other methods fails to determine the concrete {@link Event} subclass that is
	 * intended to be subscribed to.
	 *
	 * @param priority         {@link EventPriority} for this listener
	 * @param receiveCancelled Indicate if this listener should receive events that have been {@link Event#isCanceled() cancelled}
	 * @param eventType        The concrete {@link Event} subclass to subscribe to
	 * @param consumer         Callback to invoke when a matching event is received
	 * @param <T>              The {@link Event} subclass to listen for
	 */
	<T extends Event> void addListener(EventPriority priority, boolean receiveCancelled, Class<T> eventType, Consumer<T> consumer);
	/**
	 * Add a consumer listener for a {@link GenericEvent} subclass, filtered to only be called for the specified
	 * filter {@link Class}.
	 *
	 * @param genericClassFilter A {@link Class} which the {@link GenericEvent} should be filtered for
	 * @param consumer           Callback to invoke when a matching event is received
	 * @param <T>                The {@link GenericEvent} subclass to listen for
	 * @param <F>                The {@link Class} to filter the {@link GenericEvent} for
	 */
	<T extends GenericEvent<? extends F>, F> void addGenericListener(Class<F> genericClassFilter, Consumer<T> consumer);

	/**
	 * Add a consumer listener with the specified {@link EventPriority} and not receiving cancelled events,
	 * for a {@link GenericEvent} subclass, filtered to only be called for the specified
	 * filter {@link Class}.
	 *
	 * @param genericClassFilter A {@link Class} which the {@link GenericEvent} should be filtered for
	 * @param priority           {@link EventPriority} for this listener
	 * @param consumer           Callback to invoke when a matching event is received
	 * @param <T>                The {@link GenericEvent} subclass to listen for
	 * @param <F>                The {@link Class} to filter the {@link GenericEvent} for
	 */
	<T extends GenericEvent<? extends F>, F> void addGenericListener(Class<F> genericClassFilter, EventPriority priority, Consumer<T> consumer);

	/**
	 * Add a consumer listener with the specified {@link EventPriority} and potentially cancelled events,
	 * for a {@link GenericEvent} subclass, filtered to only be called for the specified
	 * filter {@link Class}.
	 *
	 * @param genericClassFilter A {@link Class} which the {@link GenericEvent} should be filtered for
	 * @param priority           {@link EventPriority} for this listener
	 * @param receiveCancelled   Indicate if this listener should receive events that have been {@link Event#isCanceled() cancelled}
	 * @param consumer           Callback to invoke when a matching event is received
	 * @param <T>                The {@link GenericEvent} subclass to listen for
	 * @param <F>                The {@link Class} to filter the {@link GenericEvent} for
	 */
	<T extends GenericEvent<? extends F>, F> void addGenericListener(Class<F> genericClassFilter, EventPriority priority, boolean receiveCancelled, Consumer<T> consumer);

	/**
	 * Add a consumer listener with the specified {@link EventPriority} and potentially cancelled events,
	 * for a {@link GenericEvent} subclass, filtered to only be called for the specified
	 * filter {@link Class}.
	 *
	 * <p>Use this method when one of the other methods fails to determine the concrete {@link GenericEvent} subclass that is
	 * intended to be subscribed to.
	 *
	 * @param genericClassFilter A {@link Class} which the {@link GenericEvent} should be filtered for
	 * @param priority           {@link EventPriority} for this listener
	 * @param receiveCancelled   Indicate if this listener should receive events that have been {@link Event#isCanceled() cancelled}
	 * @param eventType          The concrete {@link GenericEvent} subclass to subscribe to
	 * @param consumer           Callback to invoke when a matching event is received
	 * @param <T>                The {@link GenericEvent} subclass to listen for
	 * @param <F>                The {@link Class} to filter the {@link GenericEvent} for
	 */
	<T extends GenericEvent<? extends F>, F> void addGenericListener(Class<F> genericClassFilter, EventPriority priority, boolean receiveCancelled, Class<T> eventType, Consumer<T> consumer);

	/**
	 * Unregister the supplied listener from this EventBus.
	 *
	 * <p>Removes all listeners from events.
	 *
	 * <p>NOTE: Consumers can be stored in a variable if unregistration is required for the Consumer.
	 *
	 * @param object The object, {@link Class} or {@link Consumer} to unsubscribe.
	 */
	void unregister(Object object);

	/**
	 * Submit the event for dispatch to appropriate listeners.
	 *
	 * @param event The event to dispatch to listeners
	 * @return true if the event was {@link Event#isCanceled() cancelled}
	 */
	boolean post(Event event);

	/**
	 * Shuts down this event bus.
	 *
	 * <p>No future events will be fired on this event bus, so any call to {@link #post(Event)} will be a no op after this method has been invoked
	 */
	void shutdown();

	void start();
}
