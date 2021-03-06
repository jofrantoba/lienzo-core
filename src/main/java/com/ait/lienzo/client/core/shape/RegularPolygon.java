/*
 * Copyright (c) 2018 Ahome' Innovation Technologies. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ait.lienzo.client.core.shape;

import java.util.List;

import com.ait.lienzo.client.core.Attribute;
import com.ait.lienzo.client.core.Context2D;
import com.ait.lienzo.client.core.shape.json.validators.ValidationContext;
import com.ait.lienzo.client.core.shape.json.validators.ValidationException;
import com.ait.lienzo.client.core.types.BoundingBox;
import com.ait.lienzo.client.core.types.PathPartList;
import com.ait.lienzo.client.core.types.Point2DArray;
import com.ait.lienzo.client.core.util.Geometry;
import com.ait.lienzo.shared.core.types.ShapeType;
import com.google.gwt.json.client.JSONObject;

/**
 * In Euclidean geometry, a regular polygon is a polygon that is equiangular (all angles are equal in measure)
 * and equilateral (all sides have the same length).  All regular polygons fit perfectly inside a circle.
 */
public class RegularPolygon extends Shape<RegularPolygon>
{
    private final PathPartList m_list = new PathPartList();

    /**
     * Constructor. Creates an instance of a regular polygon.
     *
     * @param sides number of sides
     * @param radius size of the encompassing circle
     */
    public RegularPolygon(final int sides, final double radius)
    {
        super(ShapeType.REGULAR_POLYGON);

        setRadius(radius).setSides(sides);
    }

    public RegularPolygon(final int sides, final double radius, final double corner)
    {
        this(sides, radius);

        setCornerRadius(corner);
    }

    protected RegularPolygon(final JSONObject node, final ValidationContext ctx) throws ValidationException
    {
        super(ShapeType.REGULAR_POLYGON, node, ctx);
    }

    @Override
    public BoundingBox getBoundingBox()
    {
        final int s = getSides();

        final double r = getRadius();

        double minx = 0;

        double miny = 0;

        double maxx = 0;

        double maxy = 0;

        if ((s > 2) && (r > 0))
        {
            minx = maxx = 0;

            miny = maxy = 0 - r;

            for (int n = 1; n < s; n++)
            {
                final double x = (r * Math.sin((n * 2 * Math.PI) / s));

                final double y = (-1 * r * Math.cos((n * 2 * Math.PI) / s));

                minx = Math.min(minx, x);

                miny = Math.min(miny, y);

                maxx = Math.max(maxx, x);

                maxy = Math.max(maxy, y);
            }
        }
        return new BoundingBox(minx, miny, maxx, maxy);
    }

    /**
     * Draws this regular polygon
     *
     * @context
     */
    @Override
    protected boolean prepare(final Context2D context, final Attributes attr, final double alpha)
    {
        if (m_list.size() < 1)
        {
            if (false == parse(attr))
            {
                return false;
            }
        }
        if (m_list.size() < 1)
        {
            return false;
        }
        context.path(m_list);

        return true;
    }

    private boolean parse(final Attributes attr)
    {
        final int sides = attr.getSides();

        final double radius = attr.getRadius();

        if ((sides > 2) && (radius > 0))
        {
            m_list.M(0, 0 - radius);

            final double corner = getCornerRadius();

            if (corner <= 0)
            {
                for (int n = 1; n < sides; n++)
                {
                    final double theta = ((n * 2 * Math.PI) / sides);

                    m_list.L(radius * Math.sin(theta), -1 * radius * Math.cos(theta));
                }
                m_list.Z();
            }
            else
            {
                final Point2DArray list = new Point2DArray(0, 0 - radius);

                for (int n = 1; n < sides; n++)
                {
                    final double theta = ((n * 2 * Math.PI) / sides);

                    list.push(radius * Math.sin(theta), -1 * radius * Math.cos(theta));
                }
                Geometry.drawArcJoinedLines(m_list, list.push(0, 0 - radius), corner);
            }
            return true;
        }
        return false;
    }

    @Override
    public RegularPolygon refresh()
    {
        m_list.clear();

        return this;
    }

    /**
     * Gets this regular polygon's encompassing circle's radius.
     *
     * @return double
     */
    public double getRadius()
    {
        return getAttributes().getRadius();
    }

    /**
     * Sets the size of this regular polygon, expressed by the radius of the enclosing circle.
     *
     * @param radius
     * @return this RegularPolygon
     */
    public RegularPolygon setRadius(final double radius)
    {
        getAttributes().setRadius(radius);

        return refresh();
    }

    /**
     * Gets the number of sides this regular polygon has.
     *
     * @return int
     */
    public int getSides()
    {
        return getAttributes().getSides();
    }

    /**
     * Sets the number of sides this regular polygon has.
     *
     * @param sides
     * @return this RegularPolygon
     */
    public RegularPolygon setSides(final int sides)
    {
        getAttributes().setSides(sides);

        return refresh();
    }

    public double getCornerRadius()
    {
        return getAttributes().getCornerRadius();
    }

    public RegularPolygon setCornerRadius(final double radius)
    {
        getAttributes().setCornerRadius(radius);

        return refresh();
    }

    @Override
    public List<Attribute> getBoundingBoxAttributes()
    {
        return asAttributes(Attribute.RADIUS, Attribute.SIDES, Attribute.CORNER_RADIUS);
    }

    public static class RegularPolygonFactory extends ShapeFactory<RegularPolygon>
    {
        public RegularPolygonFactory()
        {
            super(ShapeType.REGULAR_POLYGON);

            addAttribute(Attribute.RADIUS, true);

            addAttribute(Attribute.SIDES, true);

            addAttribute(Attribute.CORNER_RADIUS);
        }

        @Override
        public RegularPolygon create(final JSONObject node, final ValidationContext ctx) throws ValidationException
        {
            return new RegularPolygon(node, ctx);
        }
    }
}
