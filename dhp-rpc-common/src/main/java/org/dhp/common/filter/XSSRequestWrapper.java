package org.dhp.common.filter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.springframework.util.StreamUtils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

@Slf4j
public class XSSRequestWrapper extends HttpServletRequestWrapper {

    private static Pattern[] patterns = new Pattern[]{
            Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("src[\r\n]*=[\r\n]*\\\'(.*?)\\\'", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("src[\r\n]*=[\r\n]*\\\"(.*?)\\\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<script(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("onload(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL)
    };

    protected HttpServletRequest request;

    public XSSRequestWrapper(HttpServletRequest servletRequest) {
        super(servletRequest);
        this.request = servletRequest;
    }

    @Override
    public String[] getParameterValues(String parameter) {
        String[] values = super.getParameterValues(parameter);

        if (values == null) {
            return null;
        }

        int count = values.length;
        String[] encodedValues = new String[count];
        for (int i = 0; i < count; i++) {
            encodedValues[i] = stripXSSAttack(values[i]);
        }

        return encodedValues;
    }

    @Override
    public String getParameter(String parameter) {
        String value = super.getParameter(parameter);
        if (StringUtils.isNotBlank(value)) {
            value = value.trim();
        }
        return stripXSSAttack(value);
    }

    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        return stripXSSAttack(value);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        //GET 请求不做xss处理
        if (!ServletFileUpload.isMultipartContent(request)) {
            //POST内容有数据
            if (request.getInputStream().available() > 0) {
                String str = StreamUtils.copyToString(request.getInputStream(), Charset.defaultCharset());
                str = stripXSSAttack(str);
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(str.getBytes());
                return new ServletInputStream() {
                    public boolean isFinished() {
                        return byteArrayInputStream.available() == 0;
                    }

                    public boolean isReady() {
                        return true;
                    }

                    public void setReadListener(ReadListener readListener) {
                    }

                    public int read() {
                        return byteArrayInputStream.read();
                    }
                };
            }
        }
        return super.getInputStream();
    }

    public String getRequestPostStr(HttpServletRequest request)
            throws IOException {
        return StreamUtils.copyToString(request.getInputStream(), Charset.defaultCharset());
    }

    private String stripXSSAttack(String value) {
        if (value != null) {
            // Avoid null characters
            value = value.replaceAll("\0", "");
            // Remove all sections that match a pattern
            for (Pattern scriptPattern : patterns) {
                value = scriptPattern.matcher(value).replaceAll("");
            }
        }
        return value;
    }
}
