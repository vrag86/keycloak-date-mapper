package info.vrag.keycloak.broker.provider;

import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Arrays;

public class DateAttributeMapper extends AbstractJsonUserAttributeMapper {

    protected static final Logger logger = Logger.getLogger(DateAttributeMapper.class);

    /**
     * Config param where name of mapping target USer attribute is stored.
     */
    public static final String CONF_USER_ATTRIBUTE = "userAttribute";
    /**
     * Config param where name of input date pattern.
     */
    public static final String CONF_DATE_INPUT_PATTERN = "dateInputPattern";
    /**
     * Config param where name of output date pattern.
     */
    public static final String CONF_DATE_OUTPUT_PATTERN = "dateOutputPattern";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    private static final String PROVIDER_ID = "date-attribute-mapper";

    public static final String[] COMPATIBLE_PROVIDERS = {ANY_PROVIDER};

    static {
        ProviderConfigProperty property;

        property = new ProviderConfigProperty();
        property.setName(CONF_JSON_FIELD);
        property.setLabel("Social Profile JSON Field Path");
        property.setHelpText("Path of field in Social provider User Profile JSON data to get value from. You can use dot notation for nesting and square brackets for array index.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(CONF_DATE_INPUT_PATTERN);
        property.setLabel("Input date pattern");
        property.setHelpText("Input date pattern (eg. yyyy-MM-dd for date 2021-01-10). See java.text.SimpleDateFormat");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(CONF_DATE_OUTPUT_PATTERN);
        property.setLabel("Output date pattern");
        property.setHelpText("Output date pattern (eg. yyyy-MM-dd for date 2021-01-10). See java.text.SimpleDateFormat");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(CONF_USER_ATTRIBUTE);
        property.setLabel("User Attribute Name");
        property.setHelpText("User attribute name to store information into.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return "Date Attribute Mapper";
    }

    @Override
    public String getDisplayType() {
        return "Date Attribute Mapper";
    }

    @Override
    public String getHelpText() {
        return "Transform date from input pattern to output pattern";
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        // Method runs on first login user (user not exists in Keycloak)
        String attribute = getAttribute(mapperModel);
        if (attribute == null) {
            return;
        }

        Object value = getJsonValue(mapperModel, context);
        if (value == null) {
            return;
        }
        if (value instanceof List) {
            logger.warn("Json value from user profile is list. Cant use list to convert date");
            return;
        }
        String convertedDate = convertDate(mapperModel, value.toString());
        if (convertedDate == null) {
            return;
        }
        context.setUserAttribute(attribute, convertedDate);
    }

    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        // Method runs on first login user (user not exists in Keycloak)
        String attribute = getAttribute(mapperModel);
        if (attribute == null) {
            return;
        }

        Object value = getJsonValue(mapperModel, context);
        if (value == null) {
            user.removeAttribute(attribute);
            return;
        }
        if (value instanceof List) {
            logger.warn("Json value from user profile is list. Cant use list to convert date");
            return;
        }
        String convertedDate = convertDate(mapperModel, value.toString());
        if (convertedDate == null) {
            user.removeAttribute(attribute);
            return;
        }
        user.setSingleAttribute(attribute, convertedDate);
    }

    private String convertDate (IdentityProviderMapperModel mapperModel, String dateStr) {
        String inputPattern = mapperModel.getConfig().get(CONF_DATE_INPUT_PATTERN);
        if (inputPattern == null || inputPattern.trim().isEmpty()) {
            logger.warnf("%s is not configured for mapper %s", CONF_DATE_INPUT_PATTERN, mapperModel.getName());
            return null;
        }

        String outputPattern = mapperModel.getConfig().get(CONF_DATE_OUTPUT_PATTERN);
        if (outputPattern == null || outputPattern.trim().isEmpty()) {
            logger.warnf("%s is not configured for mapper %s", CONF_DATE_OUTPUT_PATTERN, mapperModel.getName());
            return null;
        }

        DateFormat inputDf = new SimpleDateFormat(inputPattern);
        DateFormat outputDf = new SimpleDateFormat(outputPattern);
        String outputDate;

        try {
            outputDate = outputDf.format(inputDf.parse(dateStr));
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
        return outputDate;
    }

    private String getAttribute(IdentityProviderMapperModel mapperModel) {
        String attribute = mapperModel.getConfig().get(CONF_USER_ATTRIBUTE);
        if (attribute == null || attribute.trim().isEmpty()) {
            logger.warnf("Attribute is not configured for mapper %s", mapperModel.getName());
            return null;
        }
        attribute = attribute.trim();
        return attribute;
    }
}
