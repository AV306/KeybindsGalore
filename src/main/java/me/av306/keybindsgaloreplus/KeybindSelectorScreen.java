/*
 * This class is modified from the PSI mod created by Vazkii
 * Psi Source Code: https://github.com/Vazkii/Psi
 *
 * Psi is Open Source and distributed under the
 * Psi License: https://psi.vazkii.net/license.php
 *
 * HVB007: IDK What Part This credit refers to, if you want to know contact https://github.com/CaelTheColher as he is the maker of this mod
 * I am just updating it to 1.20.x
 */
package me.av306.keybindsgaloreplus;

import com.mojang.blaze3d.systems.RenderSystem;

import me.av306.keybindsgaloreplus.mixin.KeyBindingAccessor;
import net.minecraft.client.MinecraftClient;
//import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.*;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

public class KeybindSelectorScreen extends Screen
{
    // Configurable variables

    // Instance variables
    private int ticksInScreen = 0;
    private int selectedSector = -1;

    private InputUtil.Key conflictedKey = InputUtil.UNKNOWN_KEY;

    private final MinecraftClient mc;

    private int centreX = 0, centreY = 0;

    private float maxRadius = 0;
    private float maxExpandedRadius = 0;
    private float cancelZoneRadius = 0;

    private boolean isFirstFrame = true;

    public KeybindSelectorScreen()
    {
        super( NarratorManager.EMPTY );
        this.mc = MinecraftClient.getInstance();

        // Debug -- print all fields
        /*for ( var f : this.getClass().getFields() )
        {
            try
            {
                KeybindsGalorePlus.LOGGER.info( "{}: {}", f.getName(), f.get( this ) );
            }
            catch ( IllegalAccessException e )
            {
                KeybindsGalorePlus.LOGGER.warn( e.getMessage() );
            }
        }*/
    }

    public KeybindSelectorScreen( InputUtil.Key key )
    {
        this();

        this.setConflictedKey( key );
    }

    @Override
    public void render( DrawContext context, int mouseX, int mouseY, float delta )
    {
        super.render( context, mouseX, mouseY, delta );

        //this.selectedSlot = -1;

        // Pixel coords of screen centre
        // Only set these on the first frame
        // Side effect: If window is resized when the screen is open, the menu won't update
        if ( this.isFirstFrame )
        {
            // Set centre of screen
            this.centreX = this.width / 2;
            this.centreY = this.height / 2;

            this.maxRadius = Math.min( (this.centreX * Configurations.PIE_MENU_SCALE) - Configurations.PIE_MENU_MARGIN, (this.centreY * Configurations.PIE_MENU_SCALE) - Configurations.PIE_MENU_MARGIN );
            this.maxExpandedRadius = this.maxRadius * Configurations.EXPANSION_FACTOR_WHEN_SELECTED;
            this.cancelZoneRadius = maxRadius * Configurations.CANCEL_ZONE_SCALE;

            this.isFirstFrame = false;
        }

        // Angle of mouse, in radians from +X-axis, centred on the origin
        double mouseAngle = mouseAngle( this.centreX, this.centreY, mouseX, mouseY );

        float mouseDistanceFromCentre = MathHelper.sqrt( (mouseX - this.centreX) * (mouseX - this.centreX) +
                        (mouseY - this.centreY) * (mouseY - this.centreY) );

        // Determines how many sectors to make for the pie menu
        int numberOfSectors = KeybindManager.getConflicts( this.conflictedKey ).size();

        // Calculate the angle occupied by each sector
        float sectorAngle = (MathHelper.TAU) / numberOfSectors;

        // Get the exact sector index that is selected
        this.selectedSector = (int) (mouseAngle / sectorAngle);

        // Deselect slot if mouse is within cancel zone
        if ( mouseDistanceFromCentre <= this.cancelZoneRadius )
            this.selectedSector = -1;
        
        this.renderPieMenu( context, delta, numberOfSectors, sectorAngle );
        this.renderLabelTexts( context, delta, numberOfSectors, sectorAngle );
    }


    private void renderPieMenu( DrawContext context, float delta, int numberOfSectors, float sectorAngle )
    {
        // Setup rendering stuff
        Tessellator tess = Tessellator.getInstance();
    
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.setShader( GameRenderer::getPositionColorProgram ); // pre-1.21.2
        //RenderSystem.setShader( ShaderProgramKeys.POSITION_COLOR ); // Post-1.21.2

        BufferBuilder buf = tess.begin( VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR ); // 1.21+
        //BufferBuilder buf = tess.getBuffer(); // 1.20.6
        //buf.begin( VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR ); // 1.20.6

        float startAngle = 0;
        int vertices = Configurations.CIRCLE_VERTICES / numberOfSectors; // FP truncation here
        if ( vertices < 1 ) vertices = 1; // Make sure there's always at least 2 vertices for a visible trapezium
        for ( var sectorIndex = 0; sectorIndex < numberOfSectors; sectorIndex++ )
        {
            float outerRadius = calculateRadius( delta, numberOfSectors, sectorIndex );
            float innerRadius = this.cancelZoneRadius;
            short innerColor = Configurations.PIE_MENU_COLOR;
            short outerColor = Configurations.PIE_MENU_COLOR;

            // Lighten every other sector
            // Hardcoding lightening the inner color for a distinct visual identity or something
            if ( sectorIndex % 2 == 0 ) innerColor = outerColor += Configurations.PIE_MENU_COLOR_LIGHTEN_FACTOR;

            if ( this.selectedSector == sectorIndex )
            {
                //outerRadius *= EXPANSION_FACTOR_WHEN_SELECTED;
                innerRadius *= Configurations.EXPANSION_FACTOR_WHEN_SELECTED;
                outerColor = Configurations.PIE_MENU_HIGHLIGHT_COLOR;
            }

            if ( !Configurations.SECTOR_GRADATION ) innerColor = outerColor;

            this.drawSector( buf, startAngle, sectorAngle, vertices, innerRadius, outerRadius, innerColor, outerColor );

            startAngle += sectorAngle;
        }

        BufferRenderer.drawWithGlobalProgram( buf.end() );
        //tess.draw(); // 1.20.6
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void drawSector( BufferBuilder buf, float startAngle, float sectorAngle, int vertices, float innerRadius, float outerRadius,
                             short innerColor, short outerColor )
    {
        for ( var i = 0; i <= vertices; i++ )
        {
            float angle = startAngle + ((float) i / vertices) * sectorAngle;

            // Inner vertex
            // FIXME: is the compiler smart enough to optimise the trigo?
            buf.vertex( this.centreX + MathHelper.cos( angle ) * innerRadius, this.centreY + MathHelper.sin( angle ) * innerRadius, 0 );
            buf.color( innerColor, innerColor, innerColor, Configurations.PIE_MENU_ALPHA );
            //buf.next(); // 1.20.6

            // Outer vertex
            buf.vertex( this.centreX + MathHelper.cos( angle ) * outerRadius, this.centreY + MathHelper.sin( angle ) * outerRadius, 0 );
            buf.color( outerColor, outerColor, outerColor, Configurations.PIE_MENU_ALPHA );
            //buf.next(); // 1.20.6
        }
    }

    private float calculateRadius( float delta, int numberOfSectors, int sectorIndex )
    {
        float radius = Configurations.ANIMATE_PIE_MENU ?
                Math.max( 0f, Math.min( (this.ticksInScreen + delta - sectorIndex * 6f / numberOfSectors) * 40f, this.maxRadius ) ) :
                this.maxRadius;

        // Expand the sector if selected
        if ( this.selectedSector == sectorIndex ) radius *= Configurations.EXPANSION_FACTOR_WHEN_SELECTED;

        return radius;
    }

    private void renderLabelTexts(
            DrawContext context,
            float delta,
            int numberOfSectors, float sectorAngle
    )
    {
        for ( var sectorIndex = 0; sectorIndex < numberOfSectors; sectorIndex++ )
        {
            float radius = calculateRadius( delta, numberOfSectors, sectorIndex );
            
            float angle = (sectorIndex + 0.5f) * sectorAngle;

            // Position in the middle of the arc
            float xPos = this.centreX + MathHelper.cos( angle ) * radius;
            float yPos = this.centreY + MathHelper.sin( angle ) * radius;

            KeyBinding action = KeybindManager.getConflicts( this.conflictedKey ).get( sectorIndex );

            // The biggest nagging bug for me
            // Tells you which control category the action goes in

            String id = action.getTranslationKey();
            String actionName = Text.translatable( action.getCategory() ).getString() + ": " +
                Text.translatable( action.getTranslationKey() ).getString();

            // Read custom data for this keybind, if present
            if ( KeybindsGalorePlus.customDataManager.hasCustomData )
            {

                try
                {
                    //KeybindsGalorePlus.LOGGER.info( "Keybind ID: {}", id );
                    actionName = KeybindsGalorePlus.customDataManager.customData.get( id ).getDisplayName();
                }
                catch ( NullPointerException ignored )
                {
                    // Ignore NPE caused by missing data entry
                }
            }

            int textWidth = this.textRenderer.getWidth( actionName );

            // Which side of the screen are we on?
            if ( xPos > this.centreX ) // Right side
            {

                xPos -= Configurations.LABEL_TEXT_INSET;

                // Check text going off-screen
                if ( this.width - xPos < textWidth )
                    xPos -= textWidth - this.width + xPos;
            }
            else // Left side
            {

                xPos -= textWidth - Configurations.LABEL_TEXT_INSET;

                // Check text going off-screen
                if ( xPos < 0 ) xPos = Configurations.LABEL_TEXT_INSET;
            }

            // Move the text closer to the centre of the circle
            yPos -= Configurations.LABEL_TEXT_INSET;

            actionName = (this.selectedSector == sectorIndex ? Formatting.UNDERLINE : Formatting.RESET) + actionName;

            context.drawTextWithShadow( textRenderer, actionName, (int) xPos, (int) yPos, 0xFFFFFF );
        }
    }

    public void setConflictedKey( InputUtil.Key key )
    {
        this.conflictedKey = key;
    }

    // Returns the angle of the line bounded by the given coordinates and the mouse position from the vertical axis
    // This is why we study trigo, guys
    private static double mouseAngle( int x, int y, int mx, int my )
    {
        return (MathHelper.atan2(my - y, mx - x) + Math.PI * 2) % (Math.PI * 2);
    }

    @Override
    public void tick()
    {
        super.tick();
        
        if ( !InputUtil.isKeyPressed(
                MinecraftClient.getInstance().getWindow().getHandle(),
                this.conflictedKey.getCode()
            )
        )
        {
            // Hide the screen when the key is no longer pressed
            this.mc.setScreen( null );

            // Activate the selected binding
            if ( this.selectedSector != -1 )
            {
                KeyBinding bind = KeybindManager.getConflicts( conflictedKey ).get( this.selectedSector );

                KeybindsGalorePlus.debugLog( "Activated {} from pie menu", bind.getTranslationKey() );

                ((KeyBindingAccessor) bind).setPressed( true );
                ((KeyBindingAccessor) bind).setTimesPressed( 1 );
                //((KeyBindingAccessor) bind).invokeSetPressed( true );
            }
            else KeybindsGalorePlus.debugLog( "Pie menu closed with no selection" );
        }

        this.ticksInScreen++;
    }

    @Override
    // Don't pause the game when this screen is open
    // actually why not
    public boolean shouldPause()
    {
        return false;
    }

    // 1.20.2 onwards
    @Override
    public void renderBackground( DrawContext context, int mouseX, int mouseY, float delta )
    {
        // Remove the darkened background
        if ( Configurations.DARKENED_BACKGROUND ) super.renderBackground( context, mouseX, mouseY, delta );
    }
}
