package dev.aether.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;

public final class EspRenderer {
    // ---- VERSION-SENSITIVE BLOCK: pipelines + layers (render API changed in 1.21.5 and again in 1.21.11) ----
    private static final RenderPipeline LINES_XRAY = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
                    .withLocation("pipeline/aether_lines_xray")
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .build());

    private static RenderPipeline fillPipe(String name, DepthTestFunction f) {
        return RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                .withLocation("pipeline/" + name)
                .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false)
                .withDepthTestFunction(f)
                .withDepthWrite(false)
                .build());
    }
    private static final RenderPipeline FILL_XRAY  = fillPipe("aether_fill_xray", DepthTestFunction.NO_DEPTH_TEST);
    private static final RenderPipeline FILL_DEPTH = fillPipe("aether_fill_depth", DepthTestFunction.LEQUAL_DEPTH_TEST);

    // If RenderLayer.of / RenderSetup differ in your mappings, copy how RenderLayer.getDebugFilledBox() is built.
    private static final RenderLayer L_LINES_XRAY = RenderLayer.of("aether_lines_xray", RenderSetup.builder(LINES_XRAY).build());
    private static final RenderLayer L_FILL_XRAY  = RenderLayer.of("aether_fill_xray", RenderSetup.builder(FILL_XRAY).translucent().build());
    private static final RenderLayer L_FILL_DEPTH = RenderLayer.of("aether_fill_depth", RenderSetup.builder(FILL_DEPTH).translucent().build());
    // -------------------------------------------------------------------------------------------------------

    private static final int[][] FACES = {{0,1,2,3},{4,7,6,5},{0,4,5,1},{1,5,6,2},{2,6,7,3},{3,7,4,0}};

    public static void draw(MatrixStack m, VertexConsumerProvider.Immediate buf, Box b,
                            int rgb, int outlineA, int fillA, boolean outline, boolean fill, boolean xray) {
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, bl = rgb & 255;

        if (fill && fillA > 0) {
            VertexConsumer c = buf.getBuffer(xray ? L_FILL_XRAY : L_FILL_DEPTH);
            Matrix4f mat = m.peek().getPositionMatrix();
            float x0 = (float) b.minX, y0 = (float) b.minY, z0 = (float) b.minZ;
            float x1 = (float) b.maxX, y1 = (float) b.maxY, z1 = (float) b.maxZ;
            float[][] v = {{x0,y0,z0},{x1,y0,z0},{x1,y0,z1},{x0,y0,z1},{x0,y1,z0},{x1,y1,z0},{x1,y1,z1},{x0,y1,z1}};
            for (int[] f : FACES) for (int i : f)
                c.vertex(mat, v[i][0], v[i][1], v[i][2]).color(r, g, bl, fillA);
        }
        if (outline && outlineA > 0) {
            VertexConsumer c = buf.getBuffer(xray ? L_LINES_XRAY : RenderLayer.getLines());
            VertexRendering.drawBox(m, c, b, r / 255f, g / 255f, bl / 255f, outlineA / 255f);
        }
    }

    public static void flush(VertexConsumerProvider.Immediate buf) {
        buf.draw(L_FILL_DEPTH);
        buf.draw(L_FILL_XRAY);
        buf.draw(RenderLayer.getLines());
        buf.draw(L_LINES_XRAY);
    }
}
