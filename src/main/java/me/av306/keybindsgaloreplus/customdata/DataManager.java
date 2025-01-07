package me.av306.keybindsgaloreplus.customdata;

import me.av306.keybindsgaloreplus.KeybindsGalorePlus;

import java.io.*;
import java.nio.file.Path;
import java.util.Hashtable;

public class DataManager
{
    private final File dataFile;

    public final Hashtable<String, KeybindData> customData = new Hashtable<>();

    /**
     * True if the data file is present AND was read successfully
     */
    public boolean hasCustomData = true;

    public DataManager( Path dataFilePath, String dataFileName )
    {
        this.dataFile = dataFilePath.resolve( dataFileName ).toFile();

        if ( !this.dataFile.exists() )
        {
            this.hasCustomData = false;
            KeybindsGalorePlus.LOGGER.warn( "No custom keybind data file found!" );
            return;
        }

        this.readDataFile();
    }

    public void readDataFile()
    {
        this.hasCustomData = true;
        try ( BufferedReader fileReader = new BufferedReader( new FileReader( this.dataFile ) ); )
        {
            String line;
            String currentKeybind = null;
            while ( true )
            {
                line = fileReader.readLine();

                if ( line == null ) break;
                // Skip blank lines
                if ( line.isBlank() ) continue;

                // Indented line -- is property
                // Python moment
                if ( !line.endsWith( ":" ) )
                {
                    String[] lines = line.trim().split( "=" );
                    try
                    {
                        lines[1] = lines[1].replaceAll( "\"", "" );

                        switch ( lines[0] )
                        {
                            case "custom_name" -> this.customData.get( currentKeybind ).setDisplayName( lines[1] );
                            default -> KeybindsGalorePlus.LOGGER.info( "Unknown custom data field: {}", lines[0] );
                        }

                        KeybindsGalorePlus.LOGGER.info( "Set \"{}\"of keybind {} to \"{}\"", lines[0], currentKeybind, lines[1] );
                    }
                    catch ( ArrayIndexOutOfBoundsException oobe )
                    {
                        KeybindsGalorePlus.LOGGER.warn( "Skipped invalid data line: {}", line );
                    }
                }
                else
                {
                    // Non-indented line -- is header
                    currentKeybind = line.replace( ":", "" ).trim();
                    this.customData.put( currentKeybind, new KeybindData() );
                    KeybindsGalorePlus.LOGGER.info( "Reading custom data for keybind: {}", currentKeybind );
                }
            }

            // Finished reading file
            //this.hasCustomData = true;
            KeybindsGalorePlus.LOGGER.info( "Custom keybind data file read successfully!" );
        }
        catch ( IOException ioe )
        {
            // IOE -- usually file not found
            this.hasCustomData = false;

            KeybindsGalorePlus.LOGGER.warn( "IOException while reading custom data: {}", ioe.getMessage() );
            //ioe.printStackTrace();
        }
    }
}
