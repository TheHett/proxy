package http;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * Created by Hett on 02.09.2017.
 */
public class RangeTest {
    @Test
    public void parseRange() throws Exception {

        Assert.assertEquals("[[0, 50], [100, 150], [160, 999]]",
                Arrays.toString(Range.parseRangeHttpHeader("Range: bytes=0-50, 100-150, 160-", 1000)));
    }

    @Test
    public void testContentLength() {
        Range range = new Range(0, 99);
        Assert.assertEquals(100, range.getContentLength());
    }
}