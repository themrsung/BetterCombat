package pegasus.bettercombat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.NumberFormat;

public final class BetterCombat extends JavaPlugin {
    //
    // Constants
    //

    /**
     * Player death will be cancelled if they can pay this much.
     */
    private static final double RESPAWN_COST = 1000000d;

    /**
     * Players with a balance above this amount will not take damage.
     */
    private static final double GOD_MODE_BALANCE = 100000000d;

    /**
     * The cost per unit damage to maintain god mode.
     */
    private static final double GOD_MODE_DAMAGE_COST = 10000d;

    /**
     * The cost per unit food to maintain god mode.
     */
    private static final double GOD_MODE_FOOD_COST = 50000d;

    @Override
    public void onEnable() {

        //
        // Setup
        //

        var rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("BetterCombat failed to load. Vault API not found.");
            return;
        }

        var economy = rsp.getProvider();

        //
        // Events
        //

        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onDamage(EntityDamageEvent e) {
                if (e.isCancelled()) return;

                if (e.getEntityType() != EntityType.PLAYER) return;
                var player = (Player) e.getEntity();

                if (!economy.has(player, GOD_MODE_BALANCE)) return;

                var cost = Math.floor(GOD_MODE_DAMAGE_COST * e.getFinalDamage());

                var takeDamageCost = economy.withdrawPlayer(player, cost);
                if (!takeDamageCost.transactionSuccess()) return;

                e.setCancelled(true);

                player.sendMessage(Component
                        .text("실비 " + NumberFormat.getNumberInstance().format(cost) + "원 발생")
                        .color(TextColor.color(255, 255, 0))
                );
            }

            @EventHandler
            public void onHunger(FoodLevelChangeEvent e) {
                if (e.isCancelled()) return;

                if (e.getEntityType() != EntityType.PLAYER) return;
                var player = (Player) e.getEntity();

                if (!economy.has(player, GOD_MODE_BALANCE)) return;

                var cost = (20 - e.getFoodLevel()) * GOD_MODE_FOOD_COST;

                var takeFoodCost = economy.withdrawPlayer(player, cost);
                if (!takeFoodCost.transactionSuccess()) return;

                e.setCancelled(true);

                player.setFoodLevel(20);
                player.setSaturation(3);

                player.sendMessage(Component
                        .text("식비 " + NumberFormat.getNumberInstance().format(cost) + "원 발생")
                        .color(TextColor.color(0, 255, 0))
                );
            }

            @EventHandler
            public void onDeath(PlayerDeathEvent e) {
                if (e.isCancelled()) return;

                var player = e.getPlayer();

                if (!economy.has(player, RESPAWN_COST)) return;

                var takeRespawnCost = economy.withdrawPlayer(player, RESPAWN_COST);
                if (!takeRespawnCost.transactionSuccess()) return;

                e.setCancelled(true);

                player.sendMessage(Component
                        .text("사망보험료 " + NumberFormat.getNumberInstance().format(RESPAWN_COST) + "원 발생")
                        .color(TextColor.color(255, 0, 0))
                );
            }

        }, this);
    }
}
