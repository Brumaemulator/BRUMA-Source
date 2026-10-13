/* Shader for approximating the way RMXP does bitmap
 * blending via DirectDraw */

uniform sampler2D source;
uniform sampler2D destination;

uniform highp vec4 subRect;

uniform lowp float opacity;

varying highp vec2 v_texCoord;

void main()
{
	lowp vec4 srcFrag = texture2D(source, v_texCoord);
	lowp vec4 dstFrag = texture2D(destination, (v_texCoord - subRect.xy) * subRect.zw);

	mediump vec4 resFrag;

	mediump float co1 = srcFrag.a * opacity;
	mediump float co2 = dstFrag.a * (1.0 - co1);
	resFrag.a = co1 + co2;

	if (resFrag.a == 0.0)
		resFrag.rgb = srcFrag.rgb;
	else
		resFrag.rgb = (co1*srcFrag.rgb + co2*dstFrag.rgb) / resFrag.a;

	gl_FragColor = resFrag;
}
