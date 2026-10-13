
uniform sampler2D texture;
uniform lowp float gray;

varying highp vec2 v_texCoord;

void main()
{
	/* Sample source color */
	lowp vec4 frag = texture2D(texture, v_texCoord);

	/* Apply gray */
	frag.rgb = mix(frag.rgb, vec3(dot(frag.rgb, vec3(0.299, 0.587, 0.114))), gray);

	gl_FragColor = frag;
}
