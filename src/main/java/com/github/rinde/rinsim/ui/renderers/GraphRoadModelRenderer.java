/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.ui.renderers;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.List;

import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;

import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.Point;

/**
 * A simple {@link CanvasRenderer} for {@link GraphRoadModel}s. Instances can be
 * obtained via {@link #builder()}.
 * <p>
 * <b>Requires:</b> a {@link GraphRoadModel} in the
 * {@link com.github.rinde.rinsim.core.Simulator}.
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 */
public final class GraphRoadModelRenderer implements CanvasRenderer {
  private static final int NODE_RADIUS = 2;
  private static final Point RELATIVE_TEXT_POSITION = new Point(4, -14);
  private static final int ARROW_HEAD_SIZE = 8;
  private static final Point ARROW_REL_FROM_TO = new Point(.9, .95);

  private final GraphRoadModel model;
  private final int margin;
  private final boolean showNodes;
  private final boolean showNodeCoordinates;
  private final boolean showDirectionArrows;
  private final RenderHelper helper;

  GraphRoadModelRenderer(GraphRoadModel grm, Builder b) {
    model = grm;
    margin = b.margin;
    showNodes = b.showNodes;
    showNodeCoordinates = b.showNodeCoordinates;
    showDirectionArrows = b.showDirectionArrows;
    helper = new RenderHelper();
  }

  @Override
  public void renderStatic(GC gc, ViewPort vp) {
    helper.adapt(gc, vp);
    final Graph<? extends ConnectionData> graph = model.getGraph();

    if (showNodes) {
      for (final Point node : graph.getNodes()) {
        helper.setBackgroundSysCol(SWT.COLOR_RED);
        helper.fillCircle(node, NODE_RADIUS);
      }
    }
    if (showNodeCoordinates) {
      for (final Point node : graph.getNodes()) {
        helper.setForegroundSysCol(SWT.COLOR_GRAY);
        helper.drawString(node.toString(), node, true,
            (int) RELATIVE_TEXT_POSITION.x, (int) RELATIVE_TEXT_POSITION.y);
      }
    }

    for (final Connection<? extends ConnectionData> e : graph.getConnections()) {
      helper.setForegroundSysCol(SWT.COLOR_GRAY);
      helper.drawLine(e.from(), e.to());

      if (showDirectionArrows) {
        final double dist = Point.distance(e.from(), e.to());
        final Point f = PointUtil.on(e, dist * ARROW_REL_FROM_TO.x);
        final Point t = PointUtil.on(e, dist * ARROW_REL_FROM_TO.y);
        helper.setBackgroundSysCol(SWT.COLOR_GRAY);
        helper.drawArrow(f, t, ARROW_HEAD_SIZE, ARROW_HEAD_SIZE);
      }
    }
  }

  @Override
  public void renderDynamic(GC gc, ViewPort vp, long time) {}

  @Nullable
  @Override
  public ViewRect getViewRect() {
    checkState(!model.getGraph().isEmpty(),
        "graph may not be empty at this point");

    final List<Point> extremes = Graphs.getExtremes(model.getGraph());
    return new ViewRect(
        PointUtil.sub(extremes.get(0), margin),
        PointUtil.add(extremes.get(1), margin));
  }

  /**
   * @return A new {@link Builder} for creating {@link GraphRoadModelRenderer}
   *         instances.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A builder for creating a {@link GraphRoadModelRenderer}.
   * @author Rinde van Lon
   */
  public static final class Builder implements CanvasRendererBuilder {
    int margin;
    boolean showNodes;
    boolean showNodeCoordinates;
    boolean showDirectionArrows;

    Builder() {
      margin = 0;
      showNodes = false;
      showNodeCoordinates = false;
      showDirectionArrows = false;
    }

    /**
     * Sets the margin to display around the graph.
     * @param m The margin, in the same unit as
     *          {@link GraphRoadModel#getDistanceUnit()}.
     * @return This, as per the builder pattern.
     */
    public Builder setMargin(int m) {
      checkArgument(m >= 0);
      margin = m;
      return this;
    }

    /**
     * Draws a circle for each node in the graph.
     * @return This, as per the builder pattern.
     */
    public Builder showNodes() {
      showNodes = true;
      return this;
    }

    /**
     * Shows a label with coordinates next to each node in the graph.
     * @return This, as per the builder pattern.
     */
    public Builder showNodeCoordinates() {
      showNodeCoordinates = true;
      return this;
    }

    /**
     * Shows a label with coordinates next to each node in the graph.
     * @return This, as per the builder pattern.
     * @deprecated Use {@link #showNodeCoordinates()}.
     */
    @Deprecated
    public Builder showNodeLabels() {
      return showNodeCoordinates();
    }

    /**
     * Shows arrows for each connection in the graph, indicating the allowed
     * driving direction(s).
     * @return This, as per the builder pattern.
     */
    public Builder showDirectionArrows() {
      showDirectionArrows = true;
      return this;
    }

    @Override
    public Builder copy() {
      final Builder copy = new Builder();
      copy.margin = margin;
      copy.showNodes = showNodes;
      copy.showNodeCoordinates = showNodeCoordinates;
      copy.showDirectionArrows = showDirectionArrows;
      return copy;
    }

    @Override
    public CanvasRenderer build(ModelProvider mp) {
      return new GraphRoadModelRenderer(mp.getModel(GraphRoadModel.class), this);
    }
  }
}
