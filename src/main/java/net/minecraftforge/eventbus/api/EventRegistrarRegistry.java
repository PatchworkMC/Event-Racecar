package net.minecraftforge.eventbus.api;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.minecraftforge.eventbus.EventRegistrarRegistryImpl;

/**
 * A registry from Class to an event registrar. Event registrars manage the registration of event handlers either
 * for a Class (for static event handlers) or an Object (for instance event handlers).
 */
public interface EventRegistrarRegistry {
	/**
	 * The implementation of EventRegistrarRegistry.
	 */
	EventRegistrarRegistry INSTANCE = new EventRegistrarRegistryImpl();

	/**
	 * Registers a static event registrar to the registry.
	 *
	 * @param clazz     The class this registrar is acting on behalf of
	 * @param registrar A registrar that will register the event handlers to the provided event bus
	 */
	void registerStatic(Class<?> clazz, Consumer<IEventBus> registrar);

	/**
	 * Gets a previously registered static event registrar from the registry.
	 *
	 * @param clazz The class this registrar is acting on behalf of
	 * @return A registrar that will register the event handlers of the class to the provided event bus
	 */
	Consumer<IEventBus> getStaticRegistrar(Class<?> clazz);

	/**
	 * Registers an instance event registrar to the registry.
	 *
	 * @param clazz     The class this registrar is acting on behalf of
	 * @param registrar A registrar that will register the event handlers of the instance to the provided event bus for an instance of an object
	 * @param <T>       The type of object that will have its event handlers registered
	 */
	<T> void registerInstance(Class<T> clazz, BiConsumer<T, IEventBus> registrar);

	/**
	 * Gets a previously registered instance event registrar from the registry.
	 *
	 * @param clazz The class this registrar is acting on behalf of
	 * @return A registrar that will register the event handlers of the class to the provided event bus
	 */
	<T> BiConsumer<T, IEventBus> getInstanceRegistrar(Class<T> clazz);
}
