/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.outerj.daisy.frontend;

import org.apache.cocoon.transformation.AbstractTransformer;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.util.WildcardMatcherHelper;
import org.apache.cocoon.xml.IncludeXMLConsumer;
import org.apache.cocoon.xml.SaxBuffer;
import org.apache.cocoon.components.source.SourceUtil;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.excalibur.source.Source;
import org.apache.excalibur.xml.sax.SAXParser;
import org.apache.excalibur.store.Store;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;
import org.outerj.daisy.frontend.util.CacheHelper;
import org.outerj.daisy.frontend.util.WikiPropertiesHelper;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.io.IOException;
import java.io.File;
import java.net.URI;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URISyntaxException;

/**
 * Transformer to handle includes of non-"daisy:" URIs. The normal Cocoon include
 * transformers were not useable due to the way they handle errors (mostly not).
 */
public class ExternalIncludeTransformer extends AbstractTransformer implements Serviceable, Contextualizable {
    private static final String NAMESPACE = "http://outerx.org/daisy/1.0#externalinclude";
    private Context context;
    private ServiceManager serviceManager;
    private SourceResolver sourceResolver;
    private Request request;
    private PermissionRule[] rules;

    public void setup(SourceResolver sourceResolver, Map objectModel, String s, Parameters parameters) throws ProcessingException, SAXException, IOException {
        this.sourceResolver = sourceResolver;
        this.request = ObjectModelHelper.getRequest(objectModel);
        this.rules = null;
    }

    public void contextualize(Context context) throws ContextException {
        this.context = context;
    }

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    public void recycle() {
        this.rules = null;
        this.request = null;
        this.sourceResolver = null;
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes attributes) throws SAXException {
        if (namespaceURI.equals(NAMESPACE) && localName.equals("include")) {
            String src = attributes.getValue("src");
            src = src.trim();

            if (src == null || src.equals("")) {
                AttributesImpl attrs = new AttributesImpl();
                attrs.addAttribute("", "class", "class", "CDATA", "daisy-error");
                contentHandler.startElement("", "p", "p", attrs);
                String message = "Missing or empty src attribute on " + qName + " element";
                contentHandler.characters(message.toCharArray(), 0, message.length());
                contentHandler.endElement("", "p", "p");
                return;
            }

            if (src.startsWith("cocoon:/") && !src.startsWith("cocoon://")) {
                // a relative cocoon include, make sure it gets resolved against the 'daisy root sitemap',
                // otherwise processing these includes from e.g. extension sitemaps might give unexpected
                // results
                String daisyCocoonPath = WikiHelper.getDaisyCocoonPath(request);
                src = "cocoon:/" + daisyCocoonPath + src.substring("cocoon:".length());
            }

            Source source = null;
            try {
                boolean canRead;
                if (src.startsWith("cocoon:")) {
                    // for cocoon sources, calling getURI causes already the building of the pipeline which might
                    // have unwanted side effects
                    canRead = canRead("cocoon", src);
                } else {
                    source = sourceResolver.resolveURI(src);
                    canRead = canRead(source.getScheme(), source.getURI());
                }

                if (!canRead) {
                    throw new Exception("Inclusion of this URL is not allowed.");
                }

                if (source == null)
                    source = sourceResolver.resolveURI(src);
                SaxBuffer buffer = new SaxBuffer();
                SourceUtil.toSAX(source, buffer);
                buffer.toSAX(new IncludeXMLConsumer(xmlConsumer));
            } catch (Throwable e) {
                getLogger().error("Error processing include of " + src, e);
                AttributesImpl attrs = new AttributesImpl();
                attrs.addAttribute("", "class", "class", "CDATA", "daisy-error");
                contentHandler.startElement("", "p", "p", attrs);
                StringBuilder message = new StringBuilder("Error processing inclusion of ");
                message.append(src).append(" : ").append(e.getMessage());
                Throwable cause = e;
                while ((cause = ExceptionUtils.getCause(cause)) != null) {
                    message.append(", cause: ").append(cause.getMessage());
                }
                contentHandler.characters(message.toString().toCharArray(), 0, message.length());
                contentHandler.endElement("", "p", "p");
            }
        } else {
            super.startElement(namespaceURI, localName, qName, attributes);
        }
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        if (namespaceURI.equals(NAMESPACE) && localName.equals("include")) {
            // ignore
        } else {
            super.endElement(namespaceURI, localName, qName);
        }
    }

    private boolean canRead(String scheme, String uri) throws Exception {
        PermissionRule[] rules = getPermissionRules();
        PermissionCheckContext context = new PermissionCheckContext(scheme, uri);
        AccessOutcome outcome = new AccessOutcome();
        for (PermissionRule rule : rules) {
            try {
                rule.evaluate(context, outcome);
            } catch (Throwable e) {
                throw new Exception("Error evaluating external include permission rules.", e);
            }
        }
        return outcome.getPermission() == Permission.GRANT;
    }

    static class PermissionCheckContext {
        private String scheme;
        private String uriString;
        private String ip;
        private URI uri;

        public PermissionCheckContext(String scheme, String uriString) {
            this.scheme = scheme;
            this.uriString = uriString;
        }

        public String getScheme() {
            return scheme;
        }

        public String getUriString() {
            return uriString;
        }

        public String getIp() throws UnknownHostException, URISyntaxException {
            if (ip == null) {
                URI uri = getUri();
                String host = uri.getHost();
                InetAddress address = InetAddress.getByName(host);
                ip = address.getHostAddress();
            }
            return ip;
        }

        public URI getUri() throws URISyntaxException {
            if (uri == null) {
                uri = new URI(uriString);
            }
            return uri;
        }
    }

    static class Permission {
        String name;

        private Permission(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }

        static final Permission GRANT = new Permission("grant");
        static final Permission DENY = new Permission("deny");
    }

    static class AccessOutcome {
        Permission permission = Permission.DENY;

        public Permission getPermission() {
            return permission;
        }

        public void setPermission(Permission permission) {
            this.permission = permission;
        }
    }

    static class PermissionRule {
        private final String scheme;
        private final ValueType valueType;
        private final Matcher matcher;
        private final Permission permission;
        private final int portFrom;
        private final int portTo;

        public PermissionRule(String scheme, ValueType valueType, Matcher matcher, Permission permission, int portFrom, int portTo) {
            this.scheme = scheme;
            this.valueType = valueType;
            this.matcher = matcher;
            this.permission = permission;
            this.portFrom = portFrom;
            this.portTo = portTo;
            if ((portFrom != -1 || portTo != -1) && !scheme.equals("http"))
                throw new IllegalArgumentException("portFrom/portTo can only be used with the http scheme.");
        }

        public void evaluate(PermissionCheckContext context, AccessOutcome outcome) throws Exception {
            if (!scheme.equalsIgnoreCase(context.getScheme()))
                return;

            if (valueType != null) {
                if (matcher.matches(valueType.getValue(context), context.getUri())) {
                    if (portFrom != -1 || portTo != -1) {
                        int port = context.getUri().getPort();
                        if (port == -1)
                            port = 80;
                        if ((portFrom != -1 && port < portFrom) || (portTo != -1 && port > portTo)) {
                            return;
                        }
                    }
                    outcome.setPermission(permission);
                }
            } else {
                outcome.setPermission(permission);
            }
        }
    }

    static interface ValueType {
        String getValue(PermissionCheckContext context) throws Exception;
    }

    static interface Matcher {
        boolean matches(String value, URI uri) throws Exception;
    }

    static class StringMatcher  implements Matcher {
        private final String matchValue;

        public StringMatcher(String matchValue) {
            this.matchValue = matchValue;
        }

        public boolean matches(String value, URI uri) {
            return matchValue.equals(value);
        }
    }

    static class StringIgnoreCaseMatcher  implements Matcher {
        private final String matchValue;

        public StringIgnoreCaseMatcher(String matchValue) {
            this.matchValue = matchValue;
        }

        public boolean matches(String value, URI uri) {
            return matchValue.equalsIgnoreCase(value);
        }
    }

    static class WildcardMatcher  implements Matcher {
        private final String expression;
        

        public WildcardMatcher(String matchValue) {
            this.expression = matchValue;
        }

        public boolean matches(String value, URI uri) {
            return WildcardMatcherHelper.match(expression, value) != null;
        }
    }

    static class WildcardIgnoreCaseMatcher  implements Matcher {
        private final String expression;

        public WildcardIgnoreCaseMatcher(String matchValue) {
            this.expression = matchValue.toLowerCase();
        }

        public boolean matches(String value, URI uri) {
            return WildcardMatcherHelper.match(expression, value.toLowerCase()) != null;
        }
    }

    static class RegExpMatcher  implements Matcher {
        private final Pattern pattern;

        public RegExpMatcher(String matchValue) {
            this.pattern = Pattern.compile(matchValue);
        }

        public boolean matches(String value, URI uri) {
            java.util.regex.Matcher matcher = pattern.matcher(value);
            return matcher.matches();
        }
    }

    static class RegExpIgnoreCaseMatcher  implements Matcher {
        private final Pattern pattern;

        public RegExpIgnoreCaseMatcher(String matchValue) {
            this.pattern = Pattern.compile(matchValue, Pattern.CASE_INSENSITIVE);
        }

        public boolean matches(String value, URI uri) {
            java.util.regex.Matcher matcher = pattern.matcher(value);
            return matcher.matches();
        }
    }

    static class SubdirMatcher  implements Matcher {
        private final String matchValue;

        public SubdirMatcher(String matchValue) throws IOException {
            this.matchValue = new File(matchValue).getCanonicalPath();
        }

        public boolean matches(String value, URI uri) throws Exception {
            File file = new File(uri);
            return file.getCanonicalPath().startsWith(matchValue);
        }
    }

    static class UriValueType implements ValueType {
        public String getValue(PermissionCheckContext context) {
            return context.getUriString();
        }
    }
    private static final UriValueType URI_VALUE_TYPE = new UriValueType();

    static class HostValueType implements ValueType {
        public String getValue(PermissionCheckContext context) throws Exception {
            return context.getUri().getHost();
        }
    }
    private static final HostValueType HOST_VALUE_TYPE = new HostValueType();

    static class IpAddressValueType implements ValueType {
        public String getValue(PermissionCheckContext context) throws Exception {
            return context.getIp();
        }
    }
    private static final IpAddressValueType IP_ADDRESS_VALUE_TYPE = new IpAddressValueType();

    private PermissionRule[] getPermissionRules() throws Exception {
        loadRules();
        return this.rules;
    }

    private void loadRules() throws Exception {
        if (this.rules != null)
            return;
        
        String src = WikiPropertiesHelper.getWikiDataDir(context) + "/external-include-rules.xml";
        Source source = null;
        SAXParser parser = null;
        Store store = null;
        try {
            source = sourceResolver.resolveURI(src);
            store = (Store)serviceManager.lookup(Store.TRANSIENT_STORE);
            this.rules = (PermissionRule[]) CacheHelper.getFromCache(store, source, this.getClass().getName());
            if (this.rules == null) {
                if (!source.exists())
                    throw new Exception("No external-include-rules.xml file found.");

                parser = (SAXParser)serviceManager.lookup(SAXParser.ROLE);
                PermissionRuleBuilder builder = new PermissionRuleBuilder();
                parser.parse(SourceUtil.getInputSource(source), builder);
                this.rules = builder.getRules();
                CacheHelper.setInCache(store, rules, source, this.getClass().getName());
            }
        } finally {
            if (source != null)
                sourceResolver.release(source);
            if (parser != null)
                serviceManager.release(parser);
            if (store != null)
                serviceManager.release(store);
        }
    }

    static class PermissionRuleBuilder extends DefaultHandler {
        private List<PermissionRule> rules = new ArrayList<PermissionRule>();

        public PermissionRule[] getRules() {
            return rules.toArray(new PermissionRule[0]);
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (uri.equals("") && localName.equals("rule")) {
                try {
                    String scheme = attributes.getValue("scheme");
                    if (scheme == null || scheme.trim().length() == 0)
                        throw new SAXException("Missing or empty scheme attribute.");

                    String permission = attributes.getValue("permission");
                    if (permission == null || permission.trim().length() == 0)
                        throw new SAXException("Missing or empty permission attribute.");
                    Permission perm;
                    if (permission.equalsIgnoreCase("grant"))
                        perm = Permission.GRANT;
                    else if (permission.equalsIgnoreCase("deny"))
                        perm = Permission.DENY;
                    else
                        throw new SAXException("Invalid value for permission attribute: " + permission);

                    PermissionRule rule;
                    String value = attributes.getValue("value");
                    if (value == null || value.trim().length() == 0) {
                        rule = new PermissionRule(scheme, null, null, perm, -1, -1);
                    } else {
                        ValueType valueType;
                        if (value.equalsIgnoreCase("uri"))
                            valueType = URI_VALUE_TYPE;
                        else if (value.equalsIgnoreCase("host"))
                            valueType = HOST_VALUE_TYPE;
                        else if (value.equalsIgnoreCase("ip"))
                            valueType = IP_ADDRESS_VALUE_TYPE;
                        else
                            throw new SAXException("Invalid value for 'value' attribute: " + value);

                        String matchValue = attributes.getValue("matchValue");
                        if (matchValue == null || matchValue.trim().length() == 0)
                            throw new SAXException("Missing or empty matchValue attribute.");

                        String matchType = attributes.getValue("matchType");
                        if (matchType == null || matchType.trim().length() == 0)
                            throw new SAXException("Missing or empty matchType attribute.");
                        Matcher matcher;
                        if (matchType.equalsIgnoreCase("string"))
                            matcher = new StringMatcher(matchValue);
                        else if (matchType.equalsIgnoreCase("stringIgnoreCase"))
                            matcher = new StringIgnoreCaseMatcher(matchValue);
                        else if (matchType.equalsIgnoreCase("wildcard"))
                            matcher = new WildcardMatcher(matchValue);
                        else if (matchType.equalsIgnoreCase("wildcardIgnoreCase"))
                            matcher = new WildcardIgnoreCaseMatcher(matchValue);
                        else if (matchType.equalsIgnoreCase("regexp"))
                            matcher = new RegExpMatcher(matchValue);
                        else if (matchType.equalsIgnoreCase("regexpIgnoreCase"))
                            matcher = new RegExpIgnoreCaseMatcher(matchValue);
                        else if (matchType.equalsIgnoreCase("subdir"))
                            matcher = new SubdirMatcher(matchValue);
                        else
                            throw new SAXException("Invalid value for matchType attribute: " + matchType);

                        String portFromString = attributes.getValue("portFrom");
                        int portFrom = -1;
                        if (portFromString != null)
                            portFrom = Integer.parseInt(portFromString);

                        String portToString = attributes.getValue("portTo");
                        int portTo = -1;
                        if (portToString != null)
                            portTo = Integer.parseInt(portToString);


                        rule = new PermissionRule(scheme, valueType, matcher, perm, portFrom, portTo);
                    }
                    rules.add(rule);
                } catch (Throwable e) {
                    if (e instanceof Exception)
                        throw new SAXException("Error processing a rule.", (Exception)e);
                    else
                        throw new SAXException("Error processing a rule: " + e.getMessage());
                }
            }
        }
    }
}
