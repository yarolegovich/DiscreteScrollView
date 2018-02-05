package com.yarolegovich.discretescrollview;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Created by yarolegovich on 10/28/17.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class HorizontalDiscreteScrollLayoutManagerTest extends DiscreteScrollLayoutManagerTest {

    @Override
    protected DSVOrientation getOrientationToTest() {
        return DSVOrientation.HORIZONTAL;
    }

}
