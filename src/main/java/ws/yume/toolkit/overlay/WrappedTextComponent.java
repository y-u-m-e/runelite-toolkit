package ws.yume.toolkit.overlay;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.TextComponent;

/**
 * A line of coloured segments for an {@code OverlayPanel} that wraps instead of clipping when the
 * player makes the box narrower, and never scales its text. See {@link WrappedText} for the rules.
 *
 * <p>Segment text is plain text: RuneLite tags such as {@code <col=...>} would be measured as
 * characters, so the box would wrap early (it never clips). Colour comes from each segment.
 *
 * <p>Keep one instance as a field of the overlay ({@code OverlayPanel} clears its children each
 * frame; a new instance per frame would lose the cached layout), and use it from the overlay's
 * {@code render} on the client render thread:
 * <pre>{@code
 * private final WrappedTextComponent text = new WrappedTextComponent();
 * ...
 * text.setSegments(segments);
 * setMinimumSize(text.minimumWidth(graphics));   // the box can't go narrower than the widest word
 * panelComponent.getChildren().add(text);
 * return super.render(graphics);
 * }</pre>
 * {@code PanelComponent} gives the component its inner width (box width minus
 * {@link ComponentConstants#STANDARD_BORDER} each side) and sizes the box to the height reported
 * here, so the box always fits every line. The layout is cached and recomputed only when the
 * segments, the font or that width change.
 */
public class WrappedTextComponent implements LayoutableRenderableEntity
{
    /** Gap between segments sharing a line, in spaces of the current font. */
    private static final int SEGMENT_GAP_SPACES = 4;

    private final Rectangle bounds = new Rectangle();
    private Point preferredLocation = new Point();
    private Dimension preferredSize = new Dimension();
    private List<WrappedText.Segment> segments = Collections.emptyList();

    private WrappedText layout;
    private Font layoutFont;
    private int layoutWidth = -1;

    /** Replaces the text. Equal segments keep the cached layout. */
    public void setSegments(List<WrappedText.Segment> next)
    {
        List<WrappedText.Segment> copy = new ArrayList<>(next);
        if (!copy.equals(segments))
        {
            segments = copy;
            layout = null;
        }
    }

    /** Narrowest box width: the widest word plus the panel's border on both sides. */
    public int minimumWidth(Graphics2D graphics)
    {
        WrappedText current = layout(graphics, layoutWidth < 0 ? 0 : layoutWidth);
        return current.minimumContentWidth() + 2 * ComponentConstants.STANDARD_BORDER;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        int width = preferredSize.width > 0 ? preferredSize.width : Integer.MAX_VALUE;
        WrappedText current = layout(graphics, width);
        int lineHeight = graphics.getFontMetrics().getHeight();

        TextComponent text = new TextComponent();
        int y = preferredLocation.y;
        for (WrappedText.Line line : current.lines())
        {
            y += lineHeight;
            for (WrappedText.Run run : line.runs())
            {
                text.setPosition(preferredLocation.x + run.x(), y);
                text.setText(run.text());
                text.setColor(run.color());
                text.render(graphics);
            }
        }

        int drawnWidth = preferredSize.width > 0
            ? Math.max(preferredSize.width, current.contentWidth())
            : current.contentWidth();
        Dimension size = new Dimension(drawnWidth, current.height());
        bounds.setLocation(preferredLocation);
        bounds.setSize(size);
        return size;
    }

    private WrappedText layout(Graphics2D graphics, int width)
    {
        Font font = graphics.getFont();
        if (layout == null || !font.equals(layoutFont) || width != layoutWidth)
        {
            FontMetrics metrics = graphics.getFontMetrics();
            layout = WrappedText.layout(segments, metrics::stringWidth, metrics.getHeight(), width,
                SEGMENT_GAP_SPACES * metrics.stringWidth(" "));
            layoutFont = font;
            layoutWidth = width;
        }
        return layout;
    }

    /** The cached layout, for tests. */
    WrappedText layoutForTest()
    {
        return layout;
    }

    @Override
    public Rectangle getBounds()
    {
        return bounds;
    }

    @Override
    public void setPreferredLocation(Point location)
    {
        preferredLocation = location;
    }

    @Override
    public void setPreferredSize(Dimension size)
    {
        preferredSize = size;
    }
}
