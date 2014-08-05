/*
   Copyright (c) 2014 Ahome' Innovation Technologies. All rights reserved.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.ait.lienzo.client.core.shape;

import com.ait.lienzo.client.core.Attribute;
import com.ait.lienzo.client.core.Context2D;
import com.ait.lienzo.client.core.shape.json.IFactory;
import com.ait.lienzo.client.core.shape.json.validators.ValidationContext;
import com.ait.lienzo.client.core.shape.json.validators.ValidationException;
import com.ait.lienzo.client.core.types.NFastArrayList;
import com.ait.lienzo.shared.core.types.ShapeType;
import com.google.gwt.json.client.JSONObject;

public class SVGPath extends Shape<SVGPath>
{
    private static final char[]             COMMANDS = { 'm', 'M', 'l', 'L', 'v', 'V', 'h', 'H', 'z', 'Z', 'c', 'C', 'q', 'Q', 't', 'T', 's', 'S', 'a', 'A' };

    private String                          m_path;

    private boolean                         m_fill   = false;

    private final NFastArrayList<PathEntry> m_list   = new NFastArrayList<PathEntry>();

    public SVGPath(String path)
    {
        super(ShapeType.SVG_PATH);

        setPath(path);
    }

    protected SVGPath(JSONObject node, ValidationContext ctx) throws ValidationException
    {
        super(ShapeType.SVG_PATH, node, ctx);

        setPath(getPath());
    }

    @Override
    protected boolean prepare(Context2D context, Attributes attr, double alpha)
    {
        final int size = m_list.size();

        if (size < 1)
        {
            return false;
        }
        m_fill = false;

        context.beginPath();

        for (int i = 0; i < size; i++)
        {
            PathEntry entry = m_list.get(i);

            double[] p = entry.points;

            switch (entry.command)
            {
                case 'L':
                    context.lineTo(p[0], p[1]);
                break;
                case 'M':
                    context.moveTo(p[0], p[1]);
                break;
                case 'C':
                    context.bezierCurveTo(p[0], p[1], p[2], p[3], p[4], p[5]);
                break;
                case 'Q':
                    context.quadraticCurveTo(p[0], p[1], p[2], p[3]);
                break;
                case 'A':
                    double cx = p[0];

                    double cy = p[1];

                    double rx = p[2];

                    double ry = p[3];

                    double th = p[4];

                    double dt = p[5];

                    double ro = p[6];

                    double fs = p[7];

                    double ra = ((rx > ry) ? rx : ry);

                    double sx = ((rx > ry) ? 1 : (rx / ry));

                    double sy = ((rx > ry) ? (ry / rx) : 1);

                    context.translate(cx, cy);

                    context.rotate(ro);

                    context.scale(sx, sy);

                    context.arc(0, 0, ra, th, th + dt, (1 - fs) > 0);

                    context.scale(1 / sx, 1 / sy);

                    context.rotate(-ro);

                    context.translate(-cx, -cy);
                break;
                case 'z':
                    context.closePath();

                    m_fill = true;
                break;
            }
        }
        return true;
    }

    private final void parse(String path)
    {
        m_list.clear();

        String cs = path;

        cs = cs.replaceAll(" ", ",");

        for (int n = 0; n < COMMANDS.length; n++)
        {
            cs = cs.replaceAll("" + COMMANDS[n], "|" + COMMANDS[n]);
        }
        String[] list = cs.split("\\|");

        double cpx = 0;

        double cpy = 0;

        for (int n = 1; n < list.length; n++)
        {
            String st = list[n];

            Character ch = st.charAt(0);

            st = st.substring(1).replaceAll(",-", "-").replaceAll("-", ",-").replaceAll("e,-", "e-");

            String[] pt = st.split(",");

            int sv = 0;

            if ((pt.length > 0) && (pt[0].isEmpty()))
            {
                sv = 1;
            }
            NFastArrayList<Double> p = new NFastArrayList<Double>();

            for (int i = sv; i < pt.length; i++)
            {
                p.add(Double.valueOf(pt[i]));
            }
            while (p.size() > 0)
            {
                Character cmd = null;

                NFastArrayList<Double> points = new NFastArrayList<Double>();

                double ctx, cty;

                double rx, ry, psi, fa, fs, x1, y1;

                PathEntry prev;

                switch (ch)
                {
                    case 'l':
                        cpx += p.shift();

                        cpy += p.shift();

                        cmd = 'L';

                        points.push(cpx, cpy);
                    break;
                    case 'L':
                        cpx = p.shift();

                        cpy = p.shift();

                        points.push(cpx, cpy);
                    break;
                    case 'm':
                        double dx = p.shift();

                        double dy = p.shift();

                        cpx += dx;

                        cpy += dy;

                        cmd = 'M';

                        final int size = m_list.size();

                        if (size > 2 && m_list.get(size - 1).command == 'z')
                        {
                            for (int idx = size - 2; idx >= 0; idx--)
                            {
                                PathEntry pe = m_list.get(idx);

                                if (pe.command == 'M')
                                {
                                    cpx = pe.points[0] + dx;

                                    cpy = pe.points[1] + dy;

                                    break;
                                }
                            }
                        }
                        points.push(cpx, cpy);

                        ch = 'l';
                    break;
                    case 'M':
                        cpx = p.shift();

                        cpy = p.shift();

                        cmd = 'M';

                        points.push(cpx, cpy);

                        ch = 'L';
                    break;
                    case 'h':
                        cpx += p.shift();

                        cmd = 'L';

                        points.push(cpx, cpy);
                    break;
                    case 'H':
                        cpx = p.shift();

                        cmd = 'L';

                        points.push(cpx, cpy);
                    break;
                    case 'v':
                        cpy += p.shift();

                        cmd = 'L';

                        points.push(cpx, cpy);
                    break;
                    case 'V':
                        cpy = p.shift();

                        cmd = 'L';

                        points.push(cpx, cpy);
                    break;
                    case 'C':
                        points.push(p.shift(), p.shift(), p.shift(), p.shift());

                        cpx = p.shift();

                        cpy = p.shift();

                        points.push(cpx, cpy);
                    break;
                    case 'c':
                        points.push(cpx + p.shift(), cpy + p.shift(), cpx + p.shift(), cpy + p.shift());

                        cpx += p.shift();

                        cpy += p.shift();

                        cmd = 'C';

                        points.push(cpx, cpy);
                    break;
                    case 'S':
                        ctx = cpx;

                        cty = cpy;

                        prev = m_list.get(m_list.size() - 1);

                        if (prev.command == 'C')
                        {
                            ctx = cpx + (cpx - prev.points[2]);

                            cty = cpy + (cpy - prev.points[3]);
                        }
                        points.push(ctx, cty, p.shift(), p.shift());

                        cpx = p.shift();

                        cpy = p.shift();

                        cmd = 'C';

                        points.push(cpx, cpy);
                    break;
                    case 's':
                        ctx = cpx;

                        cty = cpy;

                        prev = m_list.get(m_list.size() - 1);

                        if (prev.command == 'C')
                        {
                            ctx = cpx + (cpx - prev.points[2]);

                            cty = cpy + (cpy - prev.points[3]);
                        }
                        points.push(ctx, cty, cpx + p.shift(), cpy + p.shift());

                        cpx += p.shift();

                        cpy += p.shift();

                        cmd = 'C';

                        points.push(cpx, cpy);
                    break;
                    case 'Q':
                        points.push(p.shift(), p.shift());

                        cpx = p.shift();

                        cpy = p.shift();

                        points.push(cpx, cpy);
                    break;
                    case 'q':
                        points.push(cpx + p.shift(), cpy + p.shift());

                        cpx += p.shift();

                        cpy += p.shift();

                        cmd = 'Q';

                        points.push(cpx, cpy);
                    break;
                    case 'T':
                        ctx = cpx;

                        cty = cpy;

                        prev = m_list.get(m_list.size() - 1);

                        if (prev.command == 'Q')
                        {
                            ctx = cpx + (cpx - prev.points[0]);

                            cty = cpy + (cpy - prev.points[1]);
                        }
                        cpx = p.shift();

                        cpy = p.shift();

                        cmd = 'Q';

                        points.push(ctx, cty, cpx, cpy);
                    break;
                    case 't':
                        ctx = cpx;

                        cty = cpy;

                        prev = m_list.get(m_list.size() - 1);

                        if (prev.command == 'Q')
                        {
                            ctx = cpx + (cpx - prev.points[0]);

                            cty = cpy + (cpy - prev.points[1]);
                        }
                        cpx += p.shift();

                        cpy += p.shift();

                        cmd = 'Q';

                        points.push(ctx, cty, cpx, cpy);
                    break;
                    case 'A':
                        rx = p.shift();

                        ry = p.shift();

                        psi = p.shift();

                        fa = p.shift();

                        fs = p.shift();

                        x1 = cpx;

                        y1 = cpy;

                        cpx = p.shift();

                        cpy = p.shift();

                        cmd = 'A';

                        points = convertEndpointToCenterParameterization(x1, y1, cpx, cpy, fa, fs, rx, ry, psi);
                    break;
                    case 'a':
                        rx = p.shift();

                        ry = p.shift();

                        psi = p.shift();

                        fa = p.shift();

                        fs = p.shift();

                        x1 = cpx;

                        y1 = cpy;

                        cpx += p.shift();

                        cpy += p.shift();

                        cmd = 'A';

                        points = convertEndpointToCenterParameterization(x1, y1, cpx, cpy, fa, fs, rx, ry, psi);
                    break;
                }
                m_list.add(new PathEntry(((null == cmd) ? ch : cmd), points));
            }
            if ((ch == 'z') || (ch == 'Z'))
            {
                m_list.add(new PathEntry('z', null));
            }
        }
    }

    private NFastArrayList<Double> convertEndpointToCenterParameterization(double x1, double y1, double x2, double y2, double fa, double fs, double rx, double ry, double psi)
    {
        psi = psi * (Math.PI / 180.0);
        double xp = Math.cos(psi) * (x1 - x2) / 2.0 + Math.sin(psi) * (y1 - y2) / 2.0;
        double yp = -1 * Math.sin(psi) * (x1 - x2) / 2.0 + Math.cos(psi) * (y1 - y2) / 2.0;

        double lambda = (xp * xp) / (rx * rx) + (yp * yp) / (ry * ry);

        if (lambda > 1)
        {
            rx *= Math.sqrt(lambda);
            ry *= Math.sqrt(lambda);
        }
        double f = Math.sqrt((((rx * rx) * (ry * ry)) - ((rx * rx) * (yp * yp)) - ((ry * ry) * (xp * xp))) / ((rx * rx) * (yp * yp) + (ry * ry) * (xp * xp)));

        if (fa == fs)
        {
            f *= -1;
        }
        if (Double.isNaN(f))
        {
            f = 0;
        }
        double cxp = f * rx * yp / ry;
        double cyp = f * -ry * xp / rx;

        double cx = (x1 + x2) / 2.0 + Math.cos(psi) * cxp - Math.sin(psi) * cyp;
        double cy = (y1 + y2) / 2.0 + Math.sin(psi) * cxp + Math.cos(psi) * cyp;

        double theta = vANGLE(new double[] { 1, 0 }, new double[] { (xp - cxp) / rx, (yp - cyp) / ry });
        double[] u = new double[] { (xp - cxp) / rx, (yp - cyp) / ry };
        double[] v = new double[] { (-1 * xp - cxp) / rx, (-1 * yp - cyp) / ry };
        double dTheta = vANGLE(u, v);

        if (vRATIO(u, v) <= -1)
        {
            dTheta = Math.PI;
        }
        if (vRATIO(u, v) >= 1)
        {
            dTheta = 0;
        }
        if (fs == 0 && dTheta > 0)
        {
            dTheta = dTheta - 2 * Math.PI;
        }
        if (fs == 1 && dTheta < 0)
        {
            dTheta = dTheta + 2 * Math.PI;
        }
        return new NFastArrayList<Double>().push(cx, cy, rx, ry, theta, dTheta, psi, fs);
    }

    private final double vMAG(double[] v)
    {
        return Math.sqrt(v[0] * v[0] + v[1] * v[1]);
    }

    private final double vRATIO(double[] u, double[] v)
    {
        return (u[0] * v[0] + u[1] * v[1]) / (vMAG(u) * vMAG(v));
    }

    private final double vANGLE(double[] u, double[] v)
    {
        return (u[0] * v[1] < u[1] * v[0] ? -1 : 1) * Math.acos(vRATIO(u, v));
    }

    @Override
    protected void fill(Context2D context, Attributes attr, double alpha)
    {
        if (m_fill)
        {
            super.fill(context, attr, alpha);
        }
    }

    public String getPath()
    {
        return getAttributes().getPath();
    }

    public SVGPath setPath(String path)
    {
        getAttributes().setPath(path);

        if (false == path.equals(m_path))
        {
            parse(m_path = path);
        }
        return this;
    }

    @Override
    public IFactory<SVGPath> getFactory()
    {
        return new SVGPathFactory();
    }

    public static class SVGPathFactory extends ShapeFactory<SVGPath>
    {
        public SVGPathFactory()
        {
            super(ShapeType.SVG_PATH);

            addAttribute(Attribute.PATH, true);
        }

        @Override
        public SVGPath create(JSONObject node, ValidationContext ctx) throws ValidationException
        {
            return new SVGPath(node, ctx);
        }
    }

    private static final class PathEntry
    {
        public final char     command;

        public final double[] points;

        private PathEntry(char c, NFastArrayList<Double> list)
        {
            command = c;

            if (null != list)
            {
                final int size = list.size();

                points = new double[size];

                for (int i = 0; i < size; i++)
                {
                    points[i] = list.get(i);
                }
            }
            else
            {
                points = new double[0];
            }
        }
    }
}
