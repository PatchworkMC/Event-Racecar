# Event Racecar: EventBus but simple

This is a fork of MinecraftForge's EventBus, modified and stripped down for use in Patchwork. The goal of this project
is to create an implementation of the Forge Event Bus that is has the same capabilities as the Forge EventBus, but
without a need for ASM, reflection, or ModLauncher. The API for the most part remains the same, however, there are some key
changes that were needed to facilitate stripping out ASM and reflection usage.


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
 

## Usage
Event Racecar is hosted on the Patchwork Maven, courtesy of [TerraformersMC](https://www.terraformersmc.com/).

```groovy
repositories {
    maven {
        url "https://maven.patchworkmc.net"
        name 'Patchwork'
    }
}

dependencies {
    // For normal use
    implementation 'net.patchworkmc:event-racecar:VERSION'
    // With TypeTools shaded (what we JiJ in patchwork-api)
    implementation 'net.patchworkmc:event-racecar:VERSION:with-typetools'
}
```
## License

Event Racecar is a fork of MinecraftForge/EventBus and is licensed under the Lesser GNU General Public License v2.


## Credits

Credits to cpw, tterrag1098, mezz, LexManos, and ichttt for creating the MinecraftForge EventBus.
