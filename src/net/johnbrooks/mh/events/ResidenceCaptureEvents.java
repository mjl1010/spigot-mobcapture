package net.johnbrooks.mh.events;

import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import net.johnbrooks.mh.Language;
import net.johnbrooks.mh.Main;
import net.johnbrooks.mh.Settings;
import net.johnbrooks.mh.events.custom.CreatureCaptureEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ResidenceCaptureEvents implements Listener {
    @EventHandler
    public void residenceCheck(CreatureCaptureEvent event) {
        if (!event.isCancelled() && Settings.residenceHook) {
            ClaimedResidence res = Main.residence.getResidenceManager().getByLoc(event.getTargetEntity().getLocation());
            if (res != null && (Main.residence.isResAdminOn(event.getCaptor()) || res.isOwner(event.getCaptor()) || res.getPermissions().playerHas(event.getCaptor(), Flags.getFlag("capture"), false))) {
                event.getCaptor().sendMessage(Language.getKey("errorCaptureAllowPermissions"));
                event.setCancelled(true);
            }
        }
    }
}
