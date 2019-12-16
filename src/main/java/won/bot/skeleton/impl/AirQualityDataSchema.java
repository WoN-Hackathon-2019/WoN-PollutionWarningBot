package won.bot.skeleton.impl;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;

public class AirQualityDataSchema {

    private static Model m = ModelFactory.createDefaultModel();

    public static final Property LOCATION = m.createProperty("http://schema.org/location");
    public static final Property CITY = m.createProperty("http://schema.org/city");
    public static final Property COUNTRY = m.createProperty("http://schema.org/country");
    public static final Property MEASUREMENTS = m.createProperty("http://schema.org/measurements"); // TODO use actual URIs?

    public static final Property MEASUREMENT = m.createProperty("http://schema.org/measurement");
    public static final Property MEASURE_PARAM = m.createProperty("http://schema.org/measure_param");
    public static final Property MEASURE_PARAM_NAME = m.createProperty("http://schema.org/measure_param_name");
    public static final Property MEASURE_VALUE = m.createProperty("http://schema.org/measure_value");
    public static final Property MEASURE_UNIT = m.createProperty("http://schema.org/measure_unit");
}
