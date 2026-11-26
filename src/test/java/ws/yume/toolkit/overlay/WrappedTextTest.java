package ws.yume.toolkit.overlay;

import static org.junit.Assert.assertEquals;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.ToIntFunction;
import org.junit.Test;

/**
 * {@link WrappedText}: segments share a line when they fit, split before words wrap, and no word
 * is ever split or clipped. Fake metrics make every character 10 px wide.
 */
public class WrappedTextTest
{
    private static final ToIntFunction<String> WIDTH = s -> s.length() * 10;
    private static final int LINE_HEIGHT = 12;
    private static final int GAP = 20;

    private static WrappedText layout(int width, String... texts)
    {
        List<WrappedText.Segment> segments = new ArrayList<>();
        for (String t : texts)
        {
            segments.add(new WrappedText.Segment(t, Color.WHITE));
        }
        return WrappedText.layout(segments, WIDTH, LINE_HEIGHT, width, GAP);
    }

    /** Each line's text, runs joined with "|". */
    private static List<String> lineTexts(WrappedText w)
    {
        List<String> out = new ArrayList<>();
        for (WrappedText.Line line : w.lines())
        {
            StringBuilder b = new StringBuilder();
            for (WrappedText.Run run : line.runs())
            {
                if (b.length() > 0)
                {
                    b.append('|');
                }
                b.append(run.text());
            }
            out.add(b.toString());
        }
        return out;
    }

    @Test
    public void segmentsShareALineWhenTheyFit()
    {
        WrappedText w = layout(200, "alpha", "12:00");
        assertEquals(Collections.singletonList("alpha|12:00"), lineTexts(w));
        assertEquals(0, w.lines().get(0).runs().get(0).x());
        assertEquals(70, w.lines().get(0).runs().get(1).x());
    }

    @Test
    public void segmentsSplitBeforeWordsWrap()
    {
        assertEquals(Arrays.asList("alpha beta", "12:00"), lineTexts(layout(110, "alpha beta", "12:00")));
    }

    @Test
    public void wordsWrapInsideASegment()
    {
        WrappedText w = layout(110, "alpha beta gamma");
        assertEquals(Arrays.asList("alpha beta", "gamma"), lineTexts(w));
        assertEquals(24, w.height());
    }

    @Test
    public void longWordStaysWholeAndSetsMinimum()
    {
        WrappedText w = layout(50, "supercalifragilistic");
        assertEquals(Collections.singletonList("supercalifragilistic"), lineTexts(w));
        assertEquals(200, w.minimumContentWidth());
    }

    @Test
    public void widthBelowMinimumLaysOutAtMinimum()
    {
        WrappedText w = layout(10, "alpha beta");
        assertEquals(Arrays.asList("alpha", "beta"), lineTexts(w));
        assertEquals(50, w.contentWidth());
    }

    @Test
    public void blankSegmentsAreSkipped()
    {
        WrappedText w = layout(200, "alpha", "", "  ");
        assertEquals(Collections.singletonList("alpha"), lineTexts(w));
        assertEquals(1, w.lines().get(0).runs().size());
    }

    @Test
    public void extraSpacesMakeNoEmptyWords()
    {
        WrappedText w = layout(200, "  alpha   beta  ");
        assertEquals(Collections.singletonList("alpha beta"), lineTexts(w));
        assertEquals(100, w.contentWidth());
    }

    @Test
    public void noSegmentsMeansNoHeight()
    {
        WrappedText w = WrappedText.layout(Collections.emptyList(), WIDTH, LINE_HEIGHT, 100, GAP);
        assertEquals(0, w.lines().size());
        assertEquals(0, w.height());
        assertEquals(0, w.minimumContentWidth());
    }
}
