package org.doublelong.catchr.entity;

import org.doublelong.catchr.Catchr;
import org.doublelong.catchr.renderer.BallRenderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;

public class Ball
{
	public final static int MAX_BOUNCE = 1;
	private final static float MIN_X = 30f;
	private final static float MAX_X = Catchr.WINDOW_WIDTH - MIN_X;
	private final Board board;

	private final CircleShape shape = new CircleShape();

	private final Body body;
	public Body getBody() { return this.body; }

	private final BodyDef bodyDef = new BodyDef();

	private final Fixture fixture;
	public Fixture getFixture() { return this.fixture; }

	private final FixtureDef fixtureDef = new FixtureDef();

	private float points = 100;
	public float getPoints() { return this.points; }
	public void setPoints(float points) { this.points = points; }

	private int bounceCount = 0;
	public int getBounceCount() { return this.bounceCount; }
	public void setBounceCount(int c) { this.bounceCount = c; }

	public BallRenderer renderer;

	private Sound sound = Gdx.audio.newSound(Gdx.files.internal("assets/sounds/zap2.mp3"));
	private long soundId;
	private float soundPitch = 1f;

	public Ball(Board board)
	{
		this.board = board;

		this.bodyDef.type = BodyType.DynamicBody;
		this.bodyDef.position.set(new Vector2(this.getRandomX(), 560f));
		this.body = this.board.getWorld().createBody(this.bodyDef);

		this.shape.setRadius(10f);
		this.fixtureDef.shape = this.shape;
		this.fixtureDef.density = .5f;
		this.fixtureDef.friction = .2f;
		this.fixtureDef.restitution = 4f;
		this.fixture = this.body.createFixture(this.fixtureDef);
		int d = (Math.random() > .5) ? -1 : 1;
		this.body.applyLinearImpulse(new Vector2(d * 10, 0f), this.body.getPosition());

		this.renderer = new BallRenderer(this);
	}

	private float getRandomX()
	{
		float r = (float) Math.random() * (MAX_X);
		if (r < MIN_X)
		{
			return MIN_X;
		}
		return r;
	}

	public void dispose()
	{
		this.shape.dispose();
		this.sound.dispose();
	}

	public void explode(ParticleEmitter emitter)
	{
		emitter.setPosition(this.body.getPosition().x, this.body.getPosition().y);
		emitter.start();
	}

	public Textr getScoreText()
	{
		Textr t = new Textr(this.body.getPosition());
		t.setMessage(String.valueOf(Math.round(this.points)) + "+");
		return t;
	}

	public void playSound(float pitch)
	{
		this.soundId = this.sound.play();
		this.sound.setPitch(this.soundId, pitch);
		this.soundPitch = this.soundPitch + .5f;
	}
}
