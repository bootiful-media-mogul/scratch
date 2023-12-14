package com.joshlong.mogul.api.managedfiles;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * includes some useful content types that, for some reason, {@link  MediaType } does not have.
 */
public abstract class CommonMediaTypes {

    private final static Logger log = LoggerFactory.getLogger(CommonMediaTypes.class);

    public static MediaType guess (Resource resource) {
        var ct = java.net.URLConnection .guessContentTypeFromName ( resource.getFilename());
        return StringUtils.hasText(ct) ?  MediaType.parseMediaType( ct ) : CommonMediaTypes.BINARY ;
    }

    /*public static MediaType tikaGuess(Resource input) {
        try {
            var is = input.getInputStream();
            log.info("is mark supported for Resource? " + is.markSupported());
            var guess = new Tika().detect(is);
            return StringUtils.hasText(guess) ?
                    MediaType.parseMediaType(guess) :
                    CommonMediaTypes.BINARY;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
*/
    public static final MediaType BINARY = MediaType.APPLICATION_OCTET_STREAM;

    // images
    public static final MediaType PNG = MediaType.IMAGE_PNG;
    public static final MediaType WEBP = MediaType.parseMediaType("image/webp");
    public static final MediaType JPG = MediaType.IMAGE_JPEG;
    public static final MediaType GIF = MediaType.IMAGE_GIF; // yuck


    // movies
    public static final MediaType MP3 = MediaType.parseMediaType("audio/mpeg");
    public static final MediaType MP4 = MediaType.parseMediaType("application/mp4");

    // audio
    public static final MediaType WAV = MediaType.parseMediaType("audio/wav");
}
