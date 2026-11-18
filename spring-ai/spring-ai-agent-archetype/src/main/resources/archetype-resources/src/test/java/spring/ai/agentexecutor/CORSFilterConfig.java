#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.spring.ai.agentexecutor;

import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Enumeration;
import java.util.List;

/**
 * >> FOR TEST ONLY <<
 * Spring Boot configuration for a custom CORS filter.
 * This is intended for testing purposes to allow requests from a local development server.
 */
@Configuration
public class CORSFilterConfig {

    @Value("${symbol_dollar}{cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public FilterRegistrationBean<Filter> corsFilter() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter((request, response, chain) -> {
            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse res = (HttpServletResponse) response;
            final var origin = req.getHeader("Origin");
            if (allowedOrigins.contains(origin)) {
                res.setHeader("Access-Control-Allow-Origin", origin);
                res.setHeader("Access-Control-Allow-Credentials", "true");
                res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
                if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
                    res.setStatus(HttpServletResponse.SC_OK);
                    return;
                }
            }
            chain.doFilter(new FakeSessionRequestWrapper(req), response);
        });
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        registration.setName("inlineCorsFilter");
        return registration;
    }

    static class FakeSessionRequestWrapper extends HttpServletRequestWrapper {

        final HttpSession session;

        public FakeSessionRequestWrapper(HttpServletRequest request) {
            super(request);
            this.session = new FakeSession(request.getServletContext());
        }

        @Override
        public HttpSession getSession(boolean create) {
            return session;
        }

        @Override
        public HttpSession getSession() {
            return session;
        }

        @Override
        public String getRequestedSessionId() {
            return session.getId();
        }
    }

    @com.fasterxml.jackson.annotation.JsonAutoDetect(fieldVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY, getterVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE, isGetterVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE, setterVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE)
    @dev.langchain4j.internal.compat.Java8Record({ "context" })
    final static class FakeSession extends dev.langchain4j.internal.compat.Java8RecordBase implements HttpSession {

        @Override
        public javax.servlet.http.HttpSessionContext getSessionContext() {
            return null;
        }

        @Override
        public Object getValue(String name) {
            return getAttribute(name);
        }

        @Override
        public String[] getValueNames() {
            return new String[0];
        }

        @Override
        public void putValue(String name, Object value) {
            setAttribute(name, value);
        }

        @Override
        public void removeValue(String name) {
            removeAttribute(name);
        }


        @Override
        public long getCreationTime() {
            return 0;
        }

        @Override
        public String getId() {
            return "SESSION1";
        }

        @Override
        public long getLastAccessedTime() {
            return 0;
        }

        @Override
        public ServletContext getServletContext() {
            return context;
        }

        @Override
        public void setMaxInactiveInterval(int i) {
        }

        @Override
        public int getMaxInactiveInterval() {
            return 0;
        }

        @Override
        public Object getAttribute(String s) {
            return null;
        }

        @Override
        public Enumeration<String> getAttributeNames() {
            return null;
        }

        @Override
        public void setAttribute(String s, Object o) {
        }

        @Override
        public void removeAttribute(String s) {
        }

        @Override
        public void invalidate() {
        }

        @Override
        public boolean isNew() {
            return false;
        }

        private final ServletContext context;

        public ServletContext context() {
            return this.context;
        }

        @com.fasterxml.jackson.annotation.JsonCreator()
        FakeSession(@com.fasterxml.jackson.annotation.JsonProperty("context") ServletContext context) {
            this.context = context;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other)
                return true;
            if (other == null || getClass() != other.getClass())
                return false;
            FakeSession that = (FakeSession) other;
            return java.util.Objects.equals(this.context, that.context);
        }

        @Override
        public int hashCode() {
            int result = 0;
            result = 31 * result + java.util.Objects.hashCode(this.context);
            return result;
        }

        @Override
        public String toString() {
            return "FakeSession[context=" + java.lang.String.valueOf((Object) this.context) + "]";
        }
    }
}
