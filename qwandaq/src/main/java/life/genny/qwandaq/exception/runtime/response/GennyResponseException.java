package life.genny.qwandaq.exception.runtime.response;

import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import life.genny.qwandaq.exception.GennyRuntimeException;
import life.genny.qwandaq.utils.LogUtils;

public class GennyResponseException extends GennyRuntimeException {
    
    String uri;
    String requestBody;
    Object responseBody;

    String contentType;
    String requestType;
    String token;

    HttpHeaders requestHeaders;
    HttpHeaders responseHeaders;
    HttpResponse<?> response;

    int statusCode;

    Exception associatedException;
    
    public GennyResponseException(String uri) {
        super("Error with response from uri: " + uri);
    }

    @Override
    public void printStackTrace() {
        log.error("Http Error for request: " + requestType + ": " + uri);
        log.error("Status Code: " + statusCode);
        if(requestBody != null) {
            log.error("=========== REQUEST BODY =========");
            log.error(requestBody);
        }
        log.debug("=========== TOKEN =========");
        log.debug(token);
		if (requestHeaders != null) {
			log.debug("======== RESPONSE HEADERS ===========");
			// LogUtils.logHeaders(log::debug, responseHeaders);
		}

        super.printStackTrace();
    }

    public static Builder newBuilder(String uri) {
        return new Builder(uri);
    }

    public static class Builder {
        private GennyResponseException exception;

        public Builder(String uri) {
            exception = new GennyResponseException(uri);
        }

        public Builder setAssociatedException(Exception e) {
            exception.associatedException = e;
            return this;
        }

        public Builder setHttpResponse(HttpResponse<?> response) {
            exception.response = response;
            return this;
        }

        public Builder setResponseHeaders(HttpHeaders responseHeaders) {
            exception.responseHeaders = responseHeaders;
            return this;
        }

        public Builder setRequestHeaders(HttpHeaders requestHeaders) {
            exception.requestHeaders = requestHeaders;
            return this;
        }

        public Builder includeRequest(HttpRequest request) {
            setRequestHeaders(request.headers());
            return this;
        }

        public Builder fromHttpResponse(HttpResponse<?> response) {
            if(response == null) {
                log.error("[!] Got NULL Response");
                return this;
            }
            setResponseBody(response.body());
            setStatusCode(response.statusCode());
            setHttpResponse(response);
            setResponseHeaders(response.headers());
            return this;
        }

        public Builder setRequestType(String requestType) {
            exception.requestType = requestType;
            return this;
        }

        public Builder setRequestBody(String requestBody) {
            exception.requestBody = requestBody;
            return this;
        }

        public Builder setURI(String uri) {
            exception.uri = uri;
            return this;
        }

        public Builder setToken(String token) {
            exception.token = token;
            return this;
        }

        public Builder setResponseBody(Object responseBody) {
            exception.responseBody = responseBody;
            return this;
        }

        public Builder setContentType(String contentType) {
            exception.contentType = contentType;
            return this;
        }

        public Builder setStatusCode(int statusCode) {
            exception.statusCode = statusCode;
            return this;
        }

        public GennyResponseException build() {
            return exception;
        }
    }
}
