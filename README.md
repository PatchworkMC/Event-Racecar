# Patchwork EventBus

This is a fork of MinecraftForge's EventBus, modified and stripped down for use in Patchwork. The goal of this project
is to create an implementation of the Forge Event Bus that is has the same capabilities as the Forge EventBus, but
without a need for any ASM or Reflection. The API for the most part remains the same, however, there are some key
changes that were needed to facilitate stripping out ASM and Reflection usage.


## What does it do?

EventBus is a library allowing users to register functions that will be called when a certain event is fired, and then
fire events to call these functions. It is very useful to notify outside code of things happening deep within Minecraft
code without every mod needing to do the same patches to the game.


## What's different from Forge?

* No annotations
    * @SubscribeEvent, @Cancelable, and @HasResult are gone
    * Cancelable and HasResult were completely unnecessary, just override isCancelable and hasResult instead.

* The IEventBus#register(Object) API has been radically altered
    * You must register a registrar with EventRegistrarRegistry before calling the register() function
    * This registrar is then responsible for registering the event handlers of the Class or Object passed in.
    * This eliminates the need for scanning classes with Reflection then using ASM to generate handlers.
 

## License

Patchwork EventBus is a fork of MinecraftForge/EventBus and is licensed under the Lesser GNU General Public License v2.