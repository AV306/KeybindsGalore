package me.av306.keybindsgaloreplus;

import me.av306.keybindsgaloreplus.mixin.KeyBindingAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import java.util.*;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.av306.keybindsgaloreplus.Configurations.DEBUG;


public class KeybindManager
{
    // To HBV007og:
    // I can't thank you enough for all the comments in the code,
    // I was worried I'd have to actually understand every line in every file
    // to do anything!
    // I hope you have fun on your modding/programming travels! :D
    // - Blender (AV306)


    /**
     * Maps physical keys to a list of bindings they can trigger.
     * <br>
     * Only contains keys bound to more than one binding.
     */
    private static final Hashtable<InputUtil.Key, List<KeyBinding>> bindingsToKeys = new Hashtable<>();

    /**
     * Check if a given key has any binding conflicts, and adds any bindings to its list.
     * <br>
     * NOTE: deprecated, bugs will not be fixed
     * 
     * @param key: The key to check
     * @return If any conflicts were found
     */
    @Deprecated
    public static boolean checkForConflicts( InputUtil.Key key )
    {
        //KeybindsGalorePlus.LOGGER.info( "Searching for conflicts..." );
        // Stop if the key is invalid; invalid keys should never end up in the map
        /*for ( InputUtil.Key illegalKey : illegalKeys )
            if ( key.equals( illegalKey ) ) return false;*/
        //if ( ILLEGAL_KEYS.contains( key ) ) return false;
        //if ( isSkippedKey( key ) ) return false;

        List<KeyBinding> matches = new ArrayList<>();


        // Look for a KeyBinding bound to the key that was just pressed
        // and add it to the running list
        for ( KeyBinding binding : MinecraftClient.getInstance().options.allKeys )
            if ( binding.matchesKey( key.getCode(), -1 ) ) matches.add( binding );

        // More than one matching KeyBinding, found conflicts!
        if ( matches.size() > 1 )
        {
            // Register the key in our map of conflicting keys
            bindingsToKeys.put( key, matches );
            //LOGGER.info("Conflicting key: " + key);

            return true;
        }
        else
        {
            // No conflicts, not worth handling
            // Remove it if it's present (means it used to be valid, but has been changed)
            bindingsToKeys.remove( key );
            return false;
        }
    }

    /**
     * FInd all conflicts on all keys
     */
    public static void getAllConflicts()
    {
        KeybindsGalorePlus.LOGGER.info( "Performing lazy conflict check" );

        MinecraftClient client = MinecraftClient.getInstance();

        // Clear map
        bindingsToKeys.clear();

        // Iterate over all bindings, adding them to the list under its assigned physical key
        for ( KeyBinding keybinding : client.options.allKeys )
        {
            InputUtil.Key physicalKey = ((KeyBindingAccessor) keybinding).getBoundKey();

            // Skip unbound keys -- keys are usually only bound to KEY_UNKNOWN when they are "unbound"
            if ( physicalKey.getCode() == GLFW.GLFW_KEY_UNKNOWN ) continue;

            //KeybindsGalorePlus.LOGGER.info( "Adding {} to list for physical key {}", keybinding.getTranslationKey(), physicalKey.getTranslationKey() );

            // Create a new list if the key doesn't have one
            bindingsToKeys.computeIfAbsent( physicalKey, key -> new ArrayList<>() );

            // Add the binding to the list held by the physical key
            bindingsToKeys.get( physicalKey ).add( keybinding );
        }

        // Prune the hashtable, copying its keys before pruning
        new HashSet<>( bindingsToKeys.keySet() ).forEach( key ->
        {
            // Remove all entries for physical keys with less than 2 bindings (they don't have conflicts)
            if ( bindingsToKeys.get( key ).size() < 2 )
                bindingsToKeys.remove( key );
        } );

        // Debug -- prints the resulting hashtable
        if ( Configurations.DEBUG )
        {
            KeybindsGalorePlus.LOGGER.info( "Dumping key conflict table" );
            bindingsToKeys.values().forEach( list -> list.forEach( binding -> KeybindsGalorePlus.LOGGER.info( "\t{} bound to physical key {}", binding.getTranslationKey(), ((KeyBindingAccessor) binding).getBoundKey() ) ) );
        }
    }

    /**
     * Does a given key NOT open a pie menu?
     */
    public static boolean isIgnoredKey( InputUtil.Key key )
    {
        return Configurations.SKIPPED_KEYS.contains( key.getCode() );
    }

    /**
     * Checks if there is a binding conflict on this key
     * @param key: The key to check
     */
    public static boolean hasConflicts( InputUtil.Key key )
    {
        return bindingsToKeys.containsKey( key );
    }

    /**
     * Initializes and open the pie menu for the given conflicted key
     */
    public static void openConflictMenu( InputUtil.Key key )
    {
        KeybindSelectorScreen screen = new KeybindSelectorScreen( key );   
        MinecraftClient.getInstance().setScreen( screen );
    }

    /**
     * Shortcut method to get conflicts on a key
     */
    public static List<KeyBinding> getConflicts( InputUtil.Key key )
    {
        return bindingsToKeys.get( key );
    }

    /**
     * Handle mixin method cancellation and related logic when a conflicted key is presed
     * @param key: the physical key that was pressed
     * @param pressed: the pressed state of the conflicted key
     * @param ci: CallbackInfo for the mixin
     */
    public static void handleKeyPress( InputUtil.Key key, boolean pressed, CallbackInfo ci )
    {
        if ( hasConflicts( key ) )
        {
            if ( !isIgnoredKey( key ) )
            {
                // Key has conflicts and shouldn't be ignored

                ci.cancel();

                if ( pressed )
                {
                    // Conflicts to handle, and was pressed -- open pie menu

                    // Changing Screens (which this method does) resets all bindings to "unpressed",
                    // so zoom mods should work absolutely fine with us :)
                    KeybindsGalorePlus.debugLog( "\tOpened pie menu" );

                    openConflictMenu( key );
                }
                // Conflicts to handle, but key was released -- do nothing
            }
            else if ( Configurations.USE_KEYBIND_FIX )
            {
                // Key conflicts ignored, and should use fixed behaviour

                ci.cancel();

                // Transfer key state to all bindings on the key
                getConflicts( key ).forEach( binding ->
                {
                    KeybindsGalorePlus.debugLog( "\tVanilla fix, enabling key {}", binding.getTranslationKey() );

                    ((KeyBindingAccessor) binding).setPressed( pressed );
                    ((KeyBindingAccessor) binding).setTimesPressed( 1 );
                } );
            }
            //else {}
            // Conflicts ignored, and vanilla behaviour is ok -- proceed as per vanilla
        }
        // else {}
        // No conflicts -- proceed as per vanilla
    }
}
