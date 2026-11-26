package ws.yume.toolkit.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import org.junit.Before;
import org.junit.Test;

/**
 * {@link WrappedTextComponent}: reports the height its wrapped lines need, a minimum width of the
 * widest word plus the panel border, and relays out when the text changes but not otherwise.
 */
public class WrappedTextComponentTest
{
    private Graphics2D g;
    private FontMetrics fm;

    @Before
    public void setUp()
    {
        g = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB).createGraphics();
        g.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        fm = g.getFontMetrics();
    }

    private static WrappedText.Segment seg(String text)
    {
        return new WrappedText.Segment(text, Color.WHITE);
    }

    @Test
    public void reportsHeightForWrappedLines()
    {
        WrappedTextComponent c = new WrappedTextComponent();
        c.setSegments(Arrays.asList(seg("alpha"), seg("bravo")));
        c.setPreferredSize(new Dimension(fm.stringWidth("bravo") + 2, 0));
        Dimension d = c.render(g);
        assertEquals(2 * fm.getHeight(), d.height);
    }

    @Test
    public void minimumWidthIsWidestWordPlusPanelBorder()
    {
        WrappedTextComponent c = new WrappedTextComponent();
        c.setSegments(Collections.singletonList(seg("a widestword b")));
        assertEquals(fm.stringWidth("widestword") + 2 * ComponentConstants.STANDARD_BORDER, c.minimumWidth(g));
    }

    @Test
    public void textChangeRelaysOut()
    {
        WrappedTextComponent c = new WrappedTextComponent();
        c.setPreferredSize(new Dimension(fm.stringWidth("alpha beta"), 0));
        c.setSegments(Collections.singletonList(seg("alpha")));
        int before = c.render(g).height;
        c.setSegments(Collections.singletonList(seg("alpha beta gamma delta")));
        int after = c.render(g).height;
        assertTrue(after > before);
    }

    @Test
    public void sameSegmentsKeepTheCachedLayout()
    {
        WrappedTextComponent c = new WrappedTextComponent();
        c.setPreferredSize(new Dimension(200, 0));
        c.setSegments(Collections.singletonList(seg("alpha")));
        c.render(g);
        WrappedText first = c.layoutForTest();
        c.setSegments(Collections.singletonList(seg("alpha")));
        c.render(g);
        assertSame(first, c.layoutForTest());
        c.setPreferredSize(new Dimension(20, 0));
        c.render(g);
        assertNotSame(first, c.layoutForTest());
    }
}
