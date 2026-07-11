package fathertoast.coopoverhaul.common.config;

import fathertoast.crust.api.config.common.ConfigUtil;
import fathertoast.crust.api.config.common.field.AbstractConfigField;
import fathertoast.crust.api.config.common.field.IntField;
import fathertoast.crust.api.config.common.file.TomlHelper;
import fathertoast.crust.api.config.common.value.HexIntWrapper;
import fathertoast.crust.api.config.common.value.collection.value.IValueCodec;
import fathertoast.crust.api.config.common.value.collection.value.IValueCorrector;

import javax.annotation.Nullable;

/**
 * A color integer value codec. Defines a default value and an allowed value range (i.e., alpha or no alpha).
 * <p>
 * TODO port this to Crust - then make this extend a new codec, HexIntValueCodec, that extends IntValueCodec
 */
@SuppressWarnings( "UnstableApiUsage" )
public class ColorIntValueCodec implements IValueCodec<Integer>, IValueCorrector<Integer> {
    
    /** The standard color integer codec for colors with an alpha (opacity) channel. Defaults to white. */
    public static final ColorIntValueCodec ALPHA = of( 0xFFFFFFFF, true );
    
    /** The standard color integer codec for colors without alpha (opacity) channel. Defaults to white. */
    public static final ColorIntValueCodec NO_ALPHA = of( 0xFFFFFF, false );
    
    public static ColorIntValueCodec of( int defaultValue, boolean useAlpha ) { return new ColorIntValueCodec( defaultValue, useAlpha ); }
    
    
    // ---- Instance Methods ---- //
    
    public final int defaultValue;
    public final int minValue;
    public final int maxValue;
    
    private ColorIntValueCodec( int def, boolean useAlpha ) {
        defaultValue = def;
        minValue = useAlpha ? IntField.Range.ANY.MIN : 0x000000;
        maxValue = useAlpha ? IntField.Range.ANY.MAX : 0xFFFFFF;
    }
    
    /** @return True if the alpha bits on this color codec are usable. */
    public boolean usesAlpha() { return maxValue == IntField.Range.ANY.MAX; }
    
    /** @return The minimum number of digits this field prints. */
    public int getMinDigits() { return usesAlpha() ? 8 : 6; } // TODO move to HexIntValueCodec
    
    /** @return The value in an appropriate hex wrapper. */
    public HexIntWrapper wrap( int value ) { return new HexIntWrapper( value, getMinDigits() ); } // TODO move to HexIntValueCodec
    
    /** @return The value format (for example, {@literal "<Number (Any Value)>"}). */
    @Override
    public String getFormat() { return getFormat( "Color" ); }
    
    /** @return The value format (for example, {@literal "<Number (Any Value)>"}). */
    public String getFormat( String name ) { // TODO move to HexIntValueCodec
        return String.format( "<%s (%s)>", name, TomlHelper.fieldRange( wrap( minValue ), wrap( maxValue ) ) );
    }
    
    /** @return The value, converted to a single-line string. */
    @Override // IValueCodec
    public String toTomlString( Integer value ) { return wrap( value ).toTomlLiteral(); } // TODO move to HexIntValueCodec
    
    /**
     * @param field The config field we are loading for, or null if error reporting should be suppressed.
     * @param line  The full line, for error context.
     * @param value The value string to parse from.
     * @return A new value based on the value string. If the parse fails, returns a non-null default value.
     */
    @Override // IValueCodec
    public Integer parseTomlString( @Nullable AbstractConfigField field, String line, @Nullable String value ) {
        if( value == null ) return defaultValue;
        if( value.startsWith( "0x" ) ) { // TODO port this to IntValueCodec
            Integer v = TomlHelper.parseHexInt( value.substring( 2 ) );
            if( v != null ) {
                return correctValue( field, line, v );
            }
        }
        else {
            Object v = TomlHelper.parseStringPrimitive( value );
            if( v instanceof Number numberValue ) {
                if( field != null && (double) numberValue.intValue() != numberValue.doubleValue() ) {
                    ConfigUtil.warnFor( field );
                    ConfigUtil.LOG.warn( "Floating point value given for integer! Truncating value {} to {}.",
                            numberValue.doubleValue(), numberValue.intValue() );
                }
                return correctValue( field, line, numberValue.intValue() );
            }
        }
        if( field != null ) {
            ConfigUtil.warnFor( field );
            ConfigUtil.LOG.warn( "Invalid integer ({})! Falling back to {}. Entry: {}",
                    value, defaultValue, line );
        }
        return defaultValue;
    }
    
    /**
     * @param field The config field we are loading for, or null if error reporting should be suppressed.
     * @param line  The full line, for error context.
     * @param value The value to correct, or null if the value is missing.
     * @return The same value if it is present and valid. If the value is missing, a default value is quietly returned.
     * If invalid, it reports the problem (unless field is null) and returns the closest valid value.
     */
    @Override // IValueCorrector
    public Integer correctValue( @Nullable AbstractConfigField field, String line, @Nullable Integer value ) {
        if( value == null ) return defaultValue;
        // Verify value is within range
        if( value < minValue ) {
            if( field != null ) {
                ConfigUtil.warnFor( field );
                ConfigUtil.LOG.warn( "Entry value is below the minimum! Adjusting from {} to {}. Entry: {}",
                        value, minValue, line );
            }
            return minValue;
        }
        else if( value > maxValue ) {
            if( field != null ) {
                ConfigUtil.warnFor( field );
                ConfigUtil.LOG.warn( "Entry value is above the maximum! Adjusting from {} to {}. Entry: {}",
                        value, maxValue, line );
            }
            return maxValue;
        }
        return value;
    }
}