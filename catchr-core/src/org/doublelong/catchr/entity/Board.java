package org.doublelong.catchr.entity;

import java.util.ArrayList;
import java.util.List;

import org.doublelong.catchr.Catchr;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

public class Board
{
	private static final float SPAWN_WAIT_TIME = 2f;
	private static final Vector2 GRAVITY = new Vector2(0, -50);
	public static float UNIT_WIDTH = Catchr.WINDOW_WIDTH / 160;
	public static float UNIT_HEIGHT = Catchr.WINDOW_HEIGHT / 160;

	private final Catchr game;
	public Catchr getGame() { return this.game; }

	private float time = 0;

	private final World world;
	public World getWorld() {return this.world; }

	private final Paddle player;
	public Paddle getPlayer() { return this.player; }

	private final Wall[] walls;

	private final List<Textr> ballTextrs = new ArrayList<Textr>();

	private int ballCount = 0;
	private int ballLimit = 4;

	private final List<Ball> balls;
	public List<Ball> getBalls() { return this.balls; }

	private final boolean debug;

	private HUD hud;

	private final ParticleEffect effect;
	private Array<ParticleEmitter> emitters;

	private Music music;
	private float pitch = 1f;
	private Sound fallOutSound;
	private long multiplier = 1;
	public long getMultiplier() { return this.multiplier; }

	public Board(Catchr game, OrthographicCamera camera, boolean debug)
	{
		this.game = game;
		this.music = game.manager.get("assets/sounds/contemplation_2.mp3", Music.class);
		this.fallOutSound = game.manager.get("assets/sounds/laser1.mp3", Sound.class);
		this.music.play();

		this.debug = debug;
		this.world = new World(GRAVITY, false);

		this.player = new Paddle(this.world, new Vector2(camera.viewportWidth / 2, 100f));

		this.balls = new ArrayList<Ball>();
		this.balls.add(new Ball(this));

		this.walls = this.generateWalls();

		this.hud = new HUD(this);

		this.effect = new ParticleEffect();
		this.effect.load(Gdx.files.internal("assets/particles/squirt2.p"), Gdx.files.internal("assets/images"));
	}

	public void dispose()
	{
		this.world.dispose();
		this.player.dispose();
		this.music.dispose();
		this.effect.dispose();
		this.fallOutSound.dispose();
	}

	public void tick(float delta)
	{
		this.time += delta;
		if (this.time >= SPAWN_WAIT_TIME)
		{
			this.spwanBall();
			this.time -= SPAWN_WAIT_TIME;
		}
	}

	public void render(SpriteBatch batch, OrthographicCamera camera, float delta)
	{
		this.hud.update(delta); // have to update first

		batch.begin();
		this.hud.render(batch, camera);
		this.player.render(batch, camera);
		for (Wall wall : this.walls)
		{
			wall.render(batch, camera);
		}
		for (int i = 0; i < this.ballTextrs.size(); i++)
		{
			Textr t = this.ballTextrs.get(i);
			if (t.getTimer() < 100)
			{
				t.render(batch, camera);
				t.update(delta);
			}
		}

		this.testCollisions(delta, batch);
		this.testFallout(batch, camera, delta);

		this.effect.draw(batch, delta);

		batch.end();
	}

	public void update(float delta)
	{
		this.player.controller.processControls();
		this.tick(delta);
		this.isDone();
	}

	private void isDone()
	{
		if (this.balls.size() == 0)
		{
			if(Gdx.input.isKeyPressed(Keys.SPACE))
			{
				this.ballCount = 0;
				this.ballLimit = 10;
			}
		}
	}

	private void spwanBall()
	{
		if (this.ballCount <= this.ballLimit)
		{
			this.balls.add(new Ball(this));
			this.ballCount++;
		}
	}

	private Wall[] generateWalls()
	{
		int numberOfWallBlocks = Math.round(Catchr.WINDOW_WIDTH / Wall.HEIGHT);
		Wall[] walls = new Wall[numberOfWallBlocks * 2]; // times 2 because we have two walls

		float y_pos = Wall.HEIGHT;
		for (int i = 0; i < numberOfWallBlocks; i++)
		{
			walls[i] = new Wall(this.world, new Vector2(Wall.WIDTH, y_pos));
			y_pos = y_pos + Wall.HEIGHT + 15;
		}

		y_pos = Wall.HEIGHT;
		for (int i = numberOfWallBlocks; i < numberOfWallBlocks * 2; i++)
		{
			walls[i] = new Wall(this.world, new Vector2(Catchr.WINDOW_WIDTH - Wall.WIDTH, y_pos));
			y_pos = y_pos + Wall.HEIGHT + 15;
		}
		return walls;
	}

	private void testCollisions(float delta, SpriteBatch batch)
	{
		List<Contact> contactList = this.world.getContactList();
		List<Ball> killList = new ArrayList<Ball>();

		for(int i = 0; i < contactList.size(); i++)
		{
			Contact contact = contactList.get(i);
			// test the contacts
			if (contact.isTouching() && (contact.getFixtureA() == this.player.getSensorFixture() || contact.getFixtureB() == this.player.getSensorFixture()))
			{
				for (int j = 0; j < this.balls.size(); j++)
				{
					Ball b =  this.balls.get(j);
					if (contact.getFixtureA() == b.getFixture() || contact.getFixtureB() == b.getFixture())
					{
						float p = b.getPoints() * this.multiplier;

						this.ballTextrs.add(b.getScoreText());
						this.player.addPoint(p);
						b.playSound(this.pitch);
						b.setPoints(p + p);
						b.setBounceCount(b.getBounceCount() + 1);
						b.renderer.changeTexture();
						if (b.getBounceCount() > Ball.MAX_BOUNCE)
						{
							killList.add(b);
							this.balls.remove(j);
							this.multiplier += 1;
							Textr t = new Textr(new Vector2(this.game.WINDOW_WIDTH / 2, this.game.WINDOW_HEIGHT / 2), 40);
							t.setMessage("x" + String.valueOf(this.multiplier));
							this.ballTextrs.add(t);
						}
						this.pitch = this.pitch + .01f;
					}
				}
			}
		}

		if (killList.size() > 0)
		{
			for(int i = 0; i < killList.size(); i++)
			{
				Ball b = killList.get(i);
				b.explode(this.effect.getEmitters().get(0));
				killList.remove(b);

				this.world.destroyBody(b.getBody());
			}
		}
	}

	private void testFallout(SpriteBatch batch, OrthographicCamera camera, float delta)
	{
		for (int i = 0; i < this.balls.size(); i++)
		{
			Ball b = this.balls.get(i);
			b.renderer.render(batch, camera);
			if (b.getBody().getPosition().y < 0)
			{
				Textr t = new Textr(new Vector2(this.game.WINDOW_WIDTH / 2, this.game.WINDOW_HEIGHT / 2), 40);
				if (this.multiplier > 1)
				{
					t.setMessage("-x" + String.valueOf(this.multiplier));
					t.setDirectrion("down");
					this.ballTextrs.add(t);
				}
				this.balls.remove(b);
				this.world.destroyBody(b.getBody());
				this.pitch = 1f;
				this.multiplier = 1;
				this.fallOutSound.play();

			}
		}
	}
}
