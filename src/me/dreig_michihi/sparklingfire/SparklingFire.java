package me.dreig_michihi.sparklingfire;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.BlueFireAbility;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.type.Candle;
import org.bukkit.block.data.type.Fire;
import org.bukkit.entity.*;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Objects;

/*
 * extends FireAbility
 * This defines what element the addon will go under. There's also ChiAbility and AvatarAbility
 * aside from each of the 4 elements and their subelements.
 *
 * implements AddonAbility
 * This is telling projectkorra that the this is an AddonAbility as opposed to
 * ComboAbility, PassiveAbility and SubAbility.
 * Notice: You should always implement AddonAbility, unless it's not an ability of course.
 * Example: For a ComboAbility you'd use "ComboAbility, AddonAbility"
 */
public class SparklingFire extends FireAbility implements AddonAbility {

    /*
     * Variables you create can go here for organization.
     * If you have your own way for organiztion, then use that.
     */
    private Listener SFL;
    private Permission perm;
    static String path = "ExtraAbilities.Dreig_Michihi.Fire.SparklingFire.";

    private Location location;
    private Location origin;
    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute(Attribute.FIRE_TICK)
    private int fireTicks;
    @Attribute(Attribute.SPEED)
    private double speed;
    @Attribute(Attribute.WIDTH)
    private double collisionRadius;
    @Attribute(Attribute.CHARGE_DURATION)
    private long chargeTime;
    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    private boolean Charged;
    private long startTime;
    private short furnaceBurnTime;
    private boolean activateCreepers;
    private boolean fillBrewingStands;
    private boolean placeFireOnEntityDamage;
    private boolean entitySpreadsFire;
    private int brewingFuel;
    private static boolean litCandles;

    /*
     * The constructor used to determine who the player is and to start the ability.
     */
    public SparklingFire(Player player) {
        super(player);

        /*
         * Doesn't allow the ability to progress if it's on cooldown.
         */
        if (bPlayer.isOnCooldown(this)) {
            return;
        }

        /*
         * Custom method that we will define later.
         */
        setFields();

        /*
         * Starts the ability.
         */
        start();

        /*
         * Puts the ability on cooldown as soon as it starts.
         */
    }

    /*
     * Place to define variables at the start of an ability.
     * Notice the "setFields()" included here and in the constructor.
     * You create variables above the constructor and here is where you define them.
     */
    private void setFields() {
        damage = ConfigManager.defaultConfig.get().getDouble(path + "Damage");
        range = ConfigManager.defaultConfig.get().getDouble(path + "Range");
        fireTicks = ConfigManager.defaultConfig.get().getInt(path + "FireTicks");
        speed = ConfigManager.defaultConfig.get().getDouble(path + "Speed");
        collisionRadius = ConfigManager.defaultConfig.get().getDouble(path + "CollisionRadius");
        cooldown = ConfigManager.defaultConfig.get().getLong(path + "Cooldown");
        chargeTime = ConfigManager.defaultConfig.get().getLong(path + "ChargeTime");
        furnaceBurnTime = (short) ConfigManager.defaultConfig.get().getInt(path + "FurnaceBurnTime");
        activateCreepers = ConfigManager.defaultConfig.get().getBoolean(path + "ActivateCreepers");
        fillBrewingStands = ConfigManager.defaultConfig.get().getBoolean(path + "FillBrewingStands");
        placeFireOnEntityDamage = ConfigManager.defaultConfig.get().getBoolean(path + "PlaceFireOnEntityDamage");
        entitySpreadsFire = ConfigManager.defaultConfig.get().getBoolean(path + "EntitySpreadsFire");
        brewingFuel = ConfigManager.defaultConfig.get().getInt(path + "BrewingFuel");
        applyModifiers(this.damage, this.range);
        this.Charged = false;
        this.startTime = System.currentTimeMillis();
        /*
         * We want to get a location that represents the start of the ability and use it for later.
         */
        this.origin = player.getLocation().clone().add(0, 1, 0);

        /*
         * Then we use another location variable so that we can tell the ability what to do.
         */
        this.location = origin.clone();

        /*
         * Since this is a "blast" ability we're going to get the players direction
         * so that we tell the ability which direction to go in.
         */
        //this.direction = player.getLocation().getDirection();
    }

    private void applyModifiers(double damage, double range) {
        int damageMod;
        int rangeMod;

        damageMod = (int) (this.getDayFactor(damage) - damage);
        rangeMod = (int) (this.getDayFactor(range) - range);

        damageMod = (int) (bPlayer.canUseSubElement(Element.SubElement.BLUE_FIRE) ? (BlueFireAbility.getDamageFactor() * damage - damage) + damageMod : damageMod);
        rangeMod = (int) (bPlayer.canUseSubElement(Element.SubElement.BLUE_FIRE) ? (BlueFireAbility.getRangeFactor() * range - range) + rangeMod : rangeMod);

        this.range += rangeMod;
        this.damage += damageMod;
    }
    /*
     * Method that controls what the abilities does.
     */

    @Override
    public void remove() {
        if (this.Charged) {
            if (bPlayer.canUseSubElement(Element.BLUE_FIRE))
                ParticleEffect.SOUL_FIRE_FLAME.display(location, 20, 0.1, 0.1, 0.1, 0.2);
            else
                ParticleEffect.FLAME.display(location, 20, 0.1, 0.1, 0.1, 0.2);
        } else {
            ParticleEffect.SMOKE_NORMAL.display(location, 10, 0.1, 0.1, 0.1, 0.05);
        }
        super.remove();
    }

    @Override
    public void progress() {
        /*
         * Makes sure the ability doesn't progress when there's no player. You could also make sure
         * they don't switch worlds.
         * English: If the player is dead or the player in not online, stop.
         */
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }
        /*
         * If the ability progresses beyond 20 blocks it will stop and be put on cooldown.
         * English: If our "origin" variable is more than 20 blocks from our "location" variable, stop.
         */
        if (origin.distance(location) > range) {
            remove();
            return;
        }
        if (isWater(this.location.getBlock())) {
            ParticleEffect.CLOUD.display(this.location, 7, 0.3, 0.3, 0.3, 0.05);
            Objects.requireNonNull(location.getWorld()).playSound(location, Sound.BLOCK_FIRE_EXTINGUISH, 1, 1);
            remove();
            return;
        }
        Vector direction;
        if (!Charged) {
            if (!bPlayer.getBoundAbilityName().equalsIgnoreCase(this.getName())) {
                remove();
                return;
            }
            direction = player.getLocation().getDirection();
            origin = player.getLocation().clone().add(0, 1, 0).add(direction);
            location = origin.clone();
            if (!player.isSneaking()) {
                if (System.currentTimeMillis() > startTime + chargeTime) {
                    Charged = true;
                } else {
                    remove();
                    return;
                }
            }
            if (!Charged && System.currentTimeMillis() > startTime + chargeTime) {
                playFirebendingParticles(origin, 1, 0.1, 0.1, 0);
                playFirebendingSound(origin);
            } else {
                ParticleEffect.SMOKE_NORMAL.display(origin, 1);
            }
        } else {
            bPlayer.addCooldown(this);
            direction = player.getEyeLocation().getDirection();
            Location oldLoc = location.clone();
            location.add(direction.clone().multiply(this.speed));
            for (double i = 0; i <= speed; i += ((i + this.collisionRadius) > speed ? speed : collisionRadius)) {
                //player.sendMessage("i: "+i+", speed: "+speed);
                location = oldLoc.clone().add(direction.clone().multiply(i));
                /*
                 * Defines the particle effect that displays at every location point.
                 * Depending on your IDE, you should be able to hover over "display" to see
                 * what each of the variabels in the paranthesis mean.
                 */

                /*
                 * Stops the ability if it hits a block.
                 * English: If the location of the ability is equal to that of a block, stop.
                 */
                playFirebendingParticles(location, 1, 0.1, 0.1, 0.1);
                if ((GeneralMethods.isSolid(location.getBlock()) || isCandle(location.getBlock()))
                        && !isWater(location.getBlock())) {
                    Block block = location.getBlock();
                    if (!GeneralMethods.isRegionProtectedFromBuild(this, location)) {
                        if (block.getType() == Material.FURNACE) {//взято из FireBlast
                            final Furnace furnace = (Furnace) block.getState();
                            furnace.setBurnTime(furnaceBurnTime);
                            furnace.update();
                            block.getWorld().playSound(block.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1, 1);
                        } else if (block.getType() == Material.SMOKER) {
                            final Smoker smoker = (Smoker) block.getState();
                            smoker.setBurnTime(furnaceBurnTime);
                            smoker.update();
                            block.getWorld().playSound(block.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1, 1);
                        } else if (block.getType() == Material.BLAST_FURNACE) {
                            final BlastFurnace blastF = (BlastFurnace) block.getState();
                            blastF.setBurnTime(furnaceBurnTime);
                            blastF.update();
                            block.getWorld().playSound(block.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1, 1);
                        } else if (block.getType() == Material.TNT) {
                            location.getBlock().setType(Material.AIR);
                            Objects.requireNonNull(block.getLocation().getWorld()).playSound(block.getLocation(), Sound.ENTITY_TNT_PRIMED, 1, 1);
                            Objects.requireNonNull(location.getWorld()).spawn(location, TNTPrimed.class);
                            block.getWorld().playSound(block.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1, 1);
                        } else if (block.getType() == Material.CAMPFIRE
                                || block.getType() == Material.SOUL_CAMPFIRE) {
                            org.bukkit.block.data.type.Campfire campfire = (org.bukkit.block.data.type.Campfire) block.getBlockData();
                            campfire.setLit(!campfire.isLit());
                            if (!campfire.isLit())
                                block.getWorld().playSound(block.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1, 1);
                            else
                                block.getWorld().playSound(block.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1, 1);
                            block.setBlockData(campfire);
                        } else if (isCandle(block)) {
                            Candle candle = (Candle) block.getBlockData();
                            candle.setLit(!candle.isLit());
                            if (!candle.isLit())
                                block.getWorld().playSound(block.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1, 1);
                            else
                                block.getWorld().playSound(block.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1, 1);
                            block.setBlockData(candle);
                        } else if (litCandles)
                            if (block.getType().equals(Material.BLACK_CANDLE_CAKE)
                                    || block.getType().equals(Material.BLUE_CANDLE_CAKE)
                                    || block.getType().equals(Material.BROWN_CANDLE_CAKE)
                                    || block.getType().equals(Material.CYAN_CANDLE_CAKE)
                                    || block.getType().equals(Material.GRAY_CANDLE_CAKE)
                                    || block.getType().equals(Material.GREEN_CANDLE_CAKE)
                                    || block.getType().equals(Material.LIGHT_BLUE_CANDLE_CAKE)
                                    || block.getType().equals(Material.LIGHT_GRAY_CANDLE_CAKE)
                                    || block.getType().equals(Material.LIME_CANDLE_CAKE)
                                    || block.getType().equals(Material.MAGENTA_CANDLE_CAKE)
                                    || block.getType().equals(Material.ORANGE_CANDLE_CAKE)
                                    || block.getType().equals(Material.PINK_CANDLE_CAKE)
                                    || block.getType().equals(Material.PURPLE_CANDLE_CAKE)
                                    || block.getType().equals(Material.RED_CANDLE_CAKE)
                                    || block.getType().equals(Material.WHITE_CANDLE_CAKE)
                                    || block.getType().equals(Material.YELLOW_CANDLE_CAKE)
                                    || block.getType().equals(Material.CANDLE_CAKE)) {
                                Lightable candle = (Lightable) block.getBlockData();
                                candle.setLit(!candle.isLit());
                                if (!candle.isLit())
                                    block.getWorld().playSound(block.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1, 1);
                                else
                                    block.getWorld().playSound(block.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1, 1);
                                block.setBlockData(candle);
                            } else if (fillBrewingStands && block.getType() == Material.BREWING_STAND) {
                                BrewingStand brewingStand = ((BrewerInventory) ((InventoryHolder) block.getState()).getInventory()).getHolder();
                                assert brewingStand != null;
                                //player.sendMessage("BrewingStand fuel lvl before:" + brewingStand.getFuelLevel());
                                brewingStand.setFuelLevel(Math.min((brewingStand.getFuelLevel() + brewingFuel), 20));
                                //player.sendMessage("BrewingStand fuel lvl after:" + brewingStand.getFuelLevel());
                                brewingStand.update();
                                ParticleEffect.FLAME.display(block.getLocation().add(.5, .5, .5), 10, 0.5, 0.5, 0.5, 0.05);
                                //block.getWorld().playSound(block.getLocation(),Sound.ITEM_FLINTANDSTEEL_USE,1,1);
                                block.getWorld().playSound(block.getLocation(), Sound.ENTITY_BLAZE_HURT, 1, 1);
                            } else {
                                if (block.getType() == Material.SOUL_SOIL || block.getType() == Material.SOUL_SAND) {
                                    if (ElementalAbility.isAir(block.getRelative(BlockFace.UP).getType()))
                                        block.getRelative(BlockFace.UP).setType(Material.SOUL_FIRE);
                                } else {
                                    if (!block.getType().isFlammable()) {
                                        if (ElementalAbility.isAir(block.getRelative(BlockFace.UP).getType()))
                                            block.getRelative(BlockFace.UP).setType(Material.FIRE);
                                    } else {
                                        BlockFace closestFace = BlockFace.SELF;
                                        BlockFace[] blockFaces = new BlockFace[]{
                                                BlockFace.UP,
                                                BlockFace.DOWN,
                                                BlockFace.EAST,
                                                BlockFace.WEST,
                                                BlockFace.NORTH,
                                                BlockFace.SOUTH};
                                        Block closest = origin.clone().add(0, 10, 0).getBlock();
                                        for (BlockFace check : blockFaces) {
                                            Block b = block.getRelative(check);
                                            if (ElementalAbility.isAir(b.getType())
                                                    && b.getLocation().add(.5, .5, .5).distanceSquared(oldLoc)
                                                    < closest.getLocation().add(.5, .5, .5).distanceSquared(oldLoc)) {
                                                closest = b;
                                                closestFace = check;
                                            }
                                        }
                                        if (closest.equals(origin.clone().add(0, 10, 0).getBlock()))
                                            closest = block.getRelative(BlockFace.UP);
                                        if (ElementalAbility.isAir(closest.getType())) {
                                            closest.setType(Material.FIRE);
                                            if (closest.getType().equals(Material.FIRE)) {
                                                Fire fire = (Fire) closest.getBlockData();
                                                if (!closestFace.equals(BlockFace.UP))
                                                    fire.setFace(closestFace.getOppositeFace(), true);
                                                closest.setBlockData(fire);
                                            }
                                        }
                                    }
                                }
                                block.getWorld().playSound(block.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1, 1);
                            }
                    }
                    remove();
                    return;
                }
                Entity entity = GeneralMethods.getClosestEntity(location, collisionRadius);
                /*
                 * The effects we apply to the "entity"
                 * English: If there is an entity which is living and is not equal to the player using the ability,
                 * the entity will take damage and the ability will stop progressing.
                 */
                if ((entity instanceof LivingEntity) && entity.getUniqueId() != player.getUniqueId()) {
                    if (activateCreepers)
                        if (entity instanceof Creeper) {
                            entity.getWorld().playSound(entity.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1, 1);
                            ((Creeper) entity).ignite();
                            remove();
                            return;
                        }
                    entity.setFireTicks(this.fireTicks * 20);
                    if (placeFireOnEntityDamage && !GeneralMethods.isRegionProtectedFromBuild(this, entity.getLocation())) {
                        if (!entity.getLocation().getBlock().getType().isSolid())
                            createTempFire(entity.getLocation());
                        if (entitySpreadsFire) {
                            long startTime = System.currentTimeMillis();
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (!entity.isDead() && entity.getFireTicks() > 0 && System.currentTimeMillis() <= startTime + 5000) {
                                        if (!entity.getLocation().getBlock().getType().isSolid())
                                            createTempFire(entity.getLocation(), 3000);
                                        ParticleEffect.FLAME.display(entity.getLocation(), 5, .5, 1.5, 0.5, 0.1);
                                    } else
                                        this.cancel();
                                }
                            }.runTaskTimer(ProjectKorra.plugin, 1L, 4L);
                        }
                    }
                    DamageHandler.damageEntity(entity, this.damage, this);
                    remove();
                    return;
                }
            }
            if (bPlayer.canUseSubElement(Element.BLUE_FIRE))
                ParticleEffect.SOUL_FIRE_FLAME.display(location, 5, 0.1, 0.1, 0, 0.3);
            else
                Objects.requireNonNull(location.getWorld()).spawnParticle(Particle.SMALL_FLAME, location, 5, 0.1, 0.1, 0, 0.3);
            //ParticleEffect.FLAME.display(location, 5, 0.1, 0.1, 0, 0.3);
            playFirebendingSound(location);
        }

        /*
         * Loop that checks for entities wherever our "location" variable is.
         * English: If there is ever an entity around the variable "location" call it "entity"
         */

    }

    public static boolean isCandle(Block block) {
        if (litCandles)
            return block.getBlockData() instanceof Candle;
        else
            return false;
    }

    /*
     * The duration of the cooldown. This is useful to some aspects of the ProjectKorra API (like bending previews)
     * and for other addon developers to use. Set this to return a 'long' variable representing your cooldown.
     */
    @Override
    public long getCooldown() {
        return this.cooldown;
    }

    /*
     * The location of the ability. This is useful for some aspects of the ProjectKorra API (like ability collisions)
     * and for other addon developers to use. Because we are not setting up any collisions and I'm not worried about
     * addon developers using this, I'm setting it to null. Otherwise, set it to return the location of your ability.
     */
    @Override
    public Location getLocation() {
        return location;
    }

    /*
     * The name of the ability.
     * This will appear when using the /bending display commands, /bending who commands, and in a BendingBoard plugin
     * you may or may not have.
     */
    @Override
    public String getName() {
        return "SparklingFire";
    }

    /*
     * The description for the ability.
     * Displays in /b h [abilityname]
     */
    @Override
    public String getDescription() {
        return "SparklingFire is a firebending technique that creates a small ball" +
                " of sparkling flame that is very easy to control.\n" +
                "This fire creates a lot of heat, so it is very easy to set" +
                " something on fire with it.\n" +
                "Using this ability, you can activate furnaces," +
                " create a block of fire," +
                " set fire to entity," +
                " explode creepers," +
                " charge brewing stands' fuel," +
                " control lit of campfires and candles," +
                " activate nether portals.";
    }

    /*
     * The instruction for the ability.
     * Displays in /b h [abilityname]
     */
    @Override
    public String getInstructions() {
        return "Hold sneak(Default Shift) to charge SparklingFire. When you see fire particles, release sneak to fire a projectile!";
    }

    /*
     * The author of the ability.
     * Displays in /b h [abilityname] in more recent versions of ProjectKorra.
     * Also useful for putting credit in the getDescription() method or the load() method if you so choose.
     */
    @Override
    public String getAuthor() {
        return "" + ChatColor.DARK_RED + ChatColor.UNDERLINE + "Dreig_Michihi";
    }

    /*
     * The version of the ability.
     * Displays in /b h [abilityname] in more recent versions of ProjectKorra.
     */
    @Override
    public String getVersion() {
        return ChatColor.GOLD + "1.3";
    }

    /*
     * Does this ability harm things?
     * This is not necessary unless you need to be concerned with whether or not this ability will work in regions.
     */
    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    /*
     * Do you need to sneak for the ability (shift)?
     * This is not necessary.
     */
    @Override
    public boolean isSneakAbility() {
        return true;
    }

    /*
     * This method is run whenever the ability is loaded into a server.
     * Restart/reload
     */
    @Override
    public void load() {
        SFL = new SparklingFireListener();
        Bukkit.getPluginManager().registerEvents(this.SFL, ProjectKorra.plugin);
        ConfigManager.defaultConfig.get().addDefault(path + "Damage", 1);
        ConfigManager.defaultConfig.get().addDefault(path + "Range", 20);
        ConfigManager.defaultConfig.get().addDefault(path + "FireTicks", 3);
        ConfigManager.defaultConfig.get().addDefault(path + "Speed", 1.5);
        ConfigManager.defaultConfig.get().addDefault(path + "CollisionRadius", 0.5);
        ConfigManager.defaultConfig.get().addDefault(path + "Cooldown", 2000);
        ConfigManager.defaultConfig.get().addDefault(path + "ChargeTime", 1000);
        ConfigManager.defaultConfig.get().addDefault(path + "FurnaceBurnTime", 800);
        ConfigManager.defaultConfig.get().addDefault(path + "ActivateCreepers", true);
        ConfigManager.defaultConfig.get().addDefault(path + "FillBrewingStands", true);
        ConfigManager.defaultConfig.get().addDefault(path + "BrewingFuel", 1);
        ConfigManager.defaultConfig.get().addDefault(path + "PlaceFireOnEntityDamage", true);
        ConfigManager.defaultConfig.get().addDefault(path + "EntitySpreadsFire", true);
        ConfigManager.defaultConfig.save();
        litCandles = !ProjectKorra.plugin.getServer().getVersion().contains("1.16");
        this.perm = new Permission("bending.ability.SparklingFire");
        this.perm.setDefault(PermissionDefault.TRUE);
        ProjectKorra.log.info(this.getName() + " by " + this.getAuthor() + " " + this.getVersion() + " has been loaded!");

    }

    /*
     * This method is run whenever the ability is disabled from a server.
     * Restart/reload
     */
    @Override
    public void stop() {
        /*
         * Log message that appears when the ability is disabled.
         */
        ProjectKorra.log.info("Successfully disabled " + getName() + " by " + getAuthor());
        ProjectKorra.plugin.getServer().getPluginManager().removePermission(this.perm);
        HandlerList.unregisterAll(SFL);
        /*
         * When the server stops or reloads, the ability will stop what it's doing and remove.
         */
        super.remove();
    }


}