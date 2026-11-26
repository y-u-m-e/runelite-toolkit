package ws.yume.toolkit.overlay;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;

/**
 * Lays fixed-size text out at a given width, wrapping instead of clipping or scaling.
 *
 * <p>Segments (each with its own colour) share one line when they all fit, separated by
 * {@code segmentGap}. If they don't, every segment starts its own line and its words fill lines
 * greedily. A word is never split: the narrowest the text can go is its widest word
 * ({@link #minimumContentWidth()}), and a requested width below that lays out at that width.
 *
 * <p>Pure and immutable; widths come from the {@code width} function (a font's
 * {@code FontMetrics::stringWidth} in practice), so any thread can call it. Padding is the
 * caller's: all widths here are content widths.
 */
public final class WrappedText
{
    /** A run of text in one colour, as the caller supplies it. */
    public static final class Segment
    {
        private final String text;
        private final Color color;

        public Segment(String text, Color color)
        {
            this.text = text == null ? "" : text;
            this.color = color;
        }

        public String text()
        {
            return text;
        }

        public Color color()
        {
            return color;
        }

        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof Segment))
            {
                return false;
            }
            Segment s = (Segment) o;
            return text.equals(s.text) && Objects.equals(color, s.color);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(text, color);
        }
    }

    /** Text to draw at {@code x}, relative to the content's left edge. */
    public static final class Run
    {
        private final String text;
        private final Color color;
        private final int x;

        Run(String text, Color color, int x)
        {
            this.text = text;
            this.color = color;
            this.x = x;
        }

        public String text()
        {
            return text;
        }

        public Color color()
        {
            return color;
        }

        public int x()
        {
            return x;
        }
    }

    /** One line of runs. */
    public static final class Line
    {
        private final List<Run> runs;
        private final int width;

        Line(List<Run> runs, int width)
        {
            this.runs = Collections.unmodifiableList(runs);
            this.width = width;
        }

        public List<Run> runs()
        {
            return runs;
        }
    }

    private final List<Line> lines;
    private final int lineHeight;
    private final int contentWidth;
    private final int minimumContentWidth;

    private WrappedText(List<Line> lines, int lineHeight, int minimumContentWidth)
    {
        this.lines = Collections.unmodifiableList(lines);
        this.lineHeight = lineHeight;
        this.minimumContentWidth = minimumContentWidth;
        int widest = 0;
        for (Line line : lines)
        {
            widest = Math.max(widest, line.width);
        }
        this.contentWidth = widest;
    }

    /**
     * @param segments in drawing order; blank ones are skipped
     * @param width text width in px
     * @param lineHeight px per line
     * @param availableWidth content width the caller offers; raised to {@link #minimumContentWidth()}
     * @param segmentGap px between segments that share a line
     */
    public static WrappedText layout(List<Segment> segments, ToIntFunction<String> width, int lineHeight,
        int availableWidth, int segmentGap)
    {
        List<Segment> used = new ArrayList<>();
        List<String[]> words = new ArrayList<>();
        int minimum = 0;
        for (Segment segment : segments)
        {
            String trimmed = segment.text().trim();
            if (trimmed.isEmpty())
            {
                continue;
            }
            String[] w = trimmed.split("\\s+");
            for (String word : w)
            {
                minimum = Math.max(minimum, width.applyAsInt(word));
            }
            used.add(segment);
            words.add(w);
        }

        int limit = Math.max(availableWidth, minimum);
        List<Line> lines = new ArrayList<>();
        if (used.isEmpty())
        {
            return new WrappedText(lines, lineHeight, 0);
        }

        // Everything on one line, if it fits.
        List<Run> runs = new ArrayList<>();
        int x = 0;
        for (int i = 0; i < used.size(); i++)
        {
            if (i > 0)
            {
                x += segmentGap;
            }
            String text = String.join(" ", words.get(i));
            runs.add(new Run(text, used.get(i).color(), x));
            x += width.applyAsInt(text);
        }
        if (x <= limit)
        {
            lines.add(new Line(runs, x));
            return new WrappedText(lines, lineHeight, minimum);
        }

        // Otherwise each segment starts a line and fills lines word by word.
        int space = width.applyAsInt(" ");
        for (int i = 0; i < used.size(); i++)
        {
            Color color = used.get(i).color();
            StringBuilder current = new StringBuilder();
            int currentWidth = 0;
            for (String word : words.get(i))
            {
                int w = width.applyAsInt(word);
                if (current.length() > 0 && currentWidth + space + w > limit)
                {
                    lines.add(new Line(Collections.singletonList(new Run(current.toString(), color, 0)), currentWidth));
                    current.setLength(0);
                    currentWidth = 0;
                }
                if (current.length() > 0)
                {
                    current.append(' ');
                    currentWidth += space;
                }
                current.append(word);
                currentWidth += w;
            }
            lines.add(new Line(Collections.singletonList(new Run(current.toString(), color, 0)), currentWidth));
        }
        return new WrappedText(lines, lineHeight, minimum);
    }

    public List<Line> lines()
    {
        return lines;
    }

    /** Total height: lines × line height, 0 when there is nothing to draw. */
    public int height()
    {
        return lines.size() * lineHeight;
    }

    /** Width of the widest laid-out line. */
    public int contentWidth()
    {
        return contentWidth;
    }

    /** Width of the widest single word: the text can't be laid out narrower. */
    public int minimumContentWidth()
    {
        return minimumContentWidth;
    }
}
