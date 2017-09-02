package http;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Created by Hett on 02.09.2017.
 */
public class RangeTest {
    @Test
    public void parseRange() throws Exception {

        Assert.assertEquals("[[0, 50], [100, 150], [160, 999]]",
                Arrays.toString(Range.parseRange("Range: bytes=0-50, 100-150, 160-", 1000)));

        Assert.assertEquals("[[200, 1000]]",
                Arrays.toString(Range.parseContentRange("Content-Range: bytes 200-1000/67589", 5000)));


    }

}