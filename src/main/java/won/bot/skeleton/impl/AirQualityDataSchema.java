package won.bot.skeleton.impl;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;

public class AirQualityDataSchema {

    private AirQualityDataSchema() {
    }

    private static Model m = ModelFactory.createDefaultModel();

    public static final Property LOCATION = m.createProperty("http://schema.org/Location");

    public static final Property PLACE = m.createProperty("http://schema.org/Place");

    public static final Property PLACE_ADDRESS = m.createProperty("http://schema.org/address");
    public static final Property PLACE_MEASUREMENT = m.createProperty("http://schema.org/amenityFeature");

    public static final Property ADDRESS = m.createProperty("http://schema.org/PostalAddress");
    public static final Property ADDR_COUNTRY = m.createProperty("http://schema.org/addressCountry");
    public static final Property ADDR_CITY = m.createProperty("http://schema.org/addressRegion");
    public static final Property ADDR_LOCALITY = m.createProperty("http://schema.org/addressLocality");

    public static final Property MEASUREMENT = m.createProperty("http://schema.org/LocationFeatureSpecification");
    public static final Property MEASURE_DATETIME = m.createProperty("http://schema.org/validFrom");
    public static final Property MEASURE_PARAM = m.createProperty("http://schema.org/propertyID");
    public static final Property MEASURE_PARAM_NAME = m.createProperty("http://schema.org/name");
    public static final Property MEASURE_VALUE = m.createProperty("http://schema.org/value");
    public static final Property MEASURE_UNIT = m.createProperty("http://schema.org/unitCode");
    public static final Property MEASURE_UNIT_TEXT = m.createProperty("http://schema.org/unitText");

    public static final Property DATETIME = m.createProperty("http://schema.org/DateTime");
}
