EasyRider
========
A Bukkit plugin that improves horses MMO-style by increasing their levels
through training.


Features
--------
 * Horses can be trained in three abilities: speed, jump strength and health.
   * **Speed**: A horse’s speed increases according to the total horizontal
     distance travelled on the ground by the horse while carrying a rider.
   * **Jump Strength**:  A horse’s jump strength increases according to the total
     horizontal distance travelled in the air by the horse while carrying a
     rider. Fall distance does not count. You can make the horse jump, ride
     it off a cliff, or just ride it up and down slopes to improve jump strength.
   * **Health**: A horse’s health increases according to the total mass of gold
     consumed in the form of golden carrots, regular golden apples and Notch
     apples. The more gold it takes to craft a food item, the more it improves
     the horse’s health. Horses eat one food item at a time. You may need to
     hurt them in order for them to eat golden apples. *What does not kill them
     makes them stronger!*
 * Horses start out at level 1 in each of these three abilities.
 * Horse attributes (health, jump and speed) improve in discrete steps when the
   level of that ability is increased to the next whole number.
 * The level of each ability has a maximum value corresponding to an attribute
   that is generally a little bit better than the best possible vanilla
   Minecraft horse in that regard. But it will take a lot of effort to get
   there. It is not possible to exceed the maximum level by further training.
 * However, admins can set the level of a horse's ability *above* the maximum
   using the `/horse-set-level` command.
 * All types of horses, including donkeys, mules, skeletal and undead horses,
   have the same maximum level and maximum ability (in this version of the
   plugin). But other vanilla restrictions remain: only donkeys and mules can
   carry chests and skeletal or undead horses cannot wear armour.
 * The total training effort to reach the next level increases *exponentially*
   with the level.
 * The maximum level, maximum significant training effort, exponential scaling,
   and the resulting range of horse attribute values are all configurable.
 * Horses have a hydration level that ranges between 0.0 and 1.0. Full hydration
   is 1.0. Hydration declines in proportion to horizontal distance travelled.
   When hydration reaches 0.0, training no longer increases the horse's levels.
 * The rate at which a horse pants (breathes heavily) increases from once a
   minute at full hydration to every four seconds at hydration 0.0.
 * Horses can be rehydrated by right clicking on them with a bucket, or by
   mounting them when they are within 3 blocks of a water block or cauldron
   containing water, either at feet level or ground level (one block below).
 * The full dehydration distance and rehydration value of a water bucket are
   configurable.
 * Players who have problems with chunks loading slowly when riding fast horses
   can set a speed limit that applies to their motion while they are riding any
   horse, using `/horse-speed-limit`. The innate maximum speed of a horse is not
   affected by this command.
 * Skeletal and undead horses that are not ridden or interacted with by their
   owner for a long time (configurable, default 14 days) may be considered
   abandoned and are then untamed and can be tamed or killed by another player,
   or killed by the environment. Naming the horse with a nametag, training it
   to level 2 or more in speed or jump, or feeding it 72 or more gold nuggets
   worth of golden food will exempt a horse from abandonment.
 * Choice of horse database implementation, as long as you choose YAML. The
   Sqlite Ebeans implementation and the combined (YAML + Sqlite) implementation
   have been dropped due to Ebeans not working as expected and the API being
   scheduled for removal from Spigot in 1.12.
 * Databases are backed up on restart in the `backups/` subdirectory of the 
   plugin folder, with a date/time stamp in the filename that has a granularity
   of 1 hour. If a matching backup already exists, it is not overwritten.


Training Algorithm
------------------
The effort, `E`, that must be expended to train an ability to an integer level,
`L`, is:

    E = K * (B ^ (L - 1) - 1)

Where:
 * `K` = effort scale factor</li>
 * `B` = effort exponential base</li>

Note that the effort to increment the level increases exponentially as the
level increases. However, as discussed below, the ability increases as a
linear function of the level. So, training gives diminishing returns as the
level increases.

The effort scale factor is chosen arbitrarily. The effort base can be
computed by substituting in the effort scale, `K`, the maximum effort, `E_max`,
and the corresponding maximum level, `L_max`:

    B = (1 + E_max / K) ^ (1 / (L_max - 1))

where the maximum level and corresponding maximum effort are carefully
selected.

The current level can be expressed as a function `L(E)` of the effort expended
in training, `E`, as:

    L(E) = min(L_max, floor(1 + ln(1 + E / K) / ln(B)))

All of these equations were in fact derived from the initial starting concept:

    L = 1 + log_B(1 + E / K)

where `log_B` signifies "logarithm to the base `B`". The logarithm of 1 is 0, so
`log_B(1 + E/K)` will always be defined and greater than 0, increasing in
proportion to effort scaled by `K`. The lowest level is 1, hence the leading
"1 +" term.

Note that the current level may be presented to the user as an integer, though
the training effort leads to a notional real number for the level. Also the
level cannot be trained past the maximum, though admins can create horses
with ability levels above the maximum.

Attributes such as speed, health (hearts) and jump strength are linearly
interpolated according to the level, from 1 to the maximum level, and
quantised to the value corresponding to `L(E)`, recalling that `L(E)` is always
rounded down to an integer.


Player Facing Commands
----------------------
 * `/horse-levels [help]` - Show level information about the horse currently
   ridden or right-clicked.

 * `/horse-owned [<page>]` - List information about all horses owned by you, optionally specifying the page number to show.

 * `/horse-upgrades health|jump|speed|help` - List all levels, corresponding
   attribute values and training effort for a specified ability.

 * `/horse-top health|jump|speed|help [<page>]` - List one page of 10 horses ranked in descending order by the specified ability. If no page number is specified, it defaults to page 1.

 * `/horse-speed-limit [help|<number>]` - Set your personal speed limit to `<number>` (in m/s), if specified, or show your current speed limit if the number is omitted.


Admin Commands
--------------
 * `/easyrider help|reload` - Show help or reload the plugin configuration.
 
 * `/easyrider migrate <type>` - Change to the specified database type. If
   a database of the new type exists, it will be rewritten to contain only the
   currently loaded horses. Currently, the only supported type is "yaml".
 
 * `/horse-debug on|off` - Turn debug logging on or off for the horse that was
   right-clicked.
   
 * `/horse-set-level health|jump|speed|help` - Set the level of the specified
   ability on the right-clicked horse.

 * `/horse-swap <partial-uuid>` - Swap the stats of the horse with the specified
   UUID with those of a clicked-on horse.

 * `/horse-owned <player> [<page>]` - List information about all horses owned by the
   specified player.


Configuration
-------------

| Setting | Description |
| :--- | :--- |
| `debug.config` | Log configuration settings on start up. |
| `debug.events` | Show extra debug messages in event handlers. |
| `debug.saves` | Enable debug logging in database saves. |
| `database.implementation` | The database implementation type to choose. Currently only "yaml" is supported. |
| `speed-limit` | The ratio of distance travelled in one tick to the current speed of a horse for its level. Used mainly as a sanity check on computed distance in movement events. But it also controls a message to players if they attempt to piston a horse way above maximum speed. |
| `dehydration-distance` | Distance a horse can travel horizontally before it is fully dehydrated. |
| `bucket-hydration` | Amount of hydration from one water bucket; 1.0 is full hydration. |
| `abandoned-days` | Number of consecutive days a horse must not be ridden or interacted with by its owner to be considered abandoned. |
| `abilities.<ability>.max-level` | The integer maximum level attainable by training. |
| `abilities.<ability>.max-effort` | The maximum amount of training effort that will be counted towards levelling up. The units of effort depend on the ability. For speed and jump, they are metres travelled horizontally on the ground or in the air, respectively. For health, the units are equivalent mass of gold nuggets consumed. |
| `abilities.<ability>.effort-scale` | The scale factor that converts the effort base, raised to the level, into required effort. |
| `abilities.<ability>.min-value` | The minimum ability value on the internal (Bukkit API) scale. |
| `abilities.<ability>.max-value` | The maximum ability value on the internal (Bukkit API) scale. |

 * In the above table, `<ability>` is each of `speed`, `jump` and `health`, in turn.


Permissions
-----------

 * `easyrider.admin` - Permission to administer the plugin (run `/easyrider reload`).
 * `easyrider.debug` - Players with this permission receive debug messages.
 * `easyrider.setlevel` - Permission to use `/horse-set-level`.
 * `easyrider.swap` - Permission to use `/horse-swap`.
 * `easyrider.levels` - Permission to use `/horse-levels`.
 * `easyrider.upgrades` - Permission to use `/horse-upgrades`.
 * `easyrider.top` - Permission to use `/horse-top`.
 * `easyrider.speedlimit` - Permission to use `/horse-speed-limit`.
 * `easyrider.owned` - Permission to use `/horse-owned`.
 * `easyrider.owned-player` - Permission to specify a player name other than one's own when using `/horse-owned`.
 
