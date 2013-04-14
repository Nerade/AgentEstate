package de.celestialcraft.util;

import static com.sk89q.worldguard.bukkit.BukkitUtil.toVector;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.bukkit.Location;
import org.bukkit.block.Sign;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import de.celestialcraft.AgentEstate.AgentEstate;

public class SignUtil {

	AgentEstate plugin;
	
	public SignUtil(AgentEstate instance) {
		plugin=instance;
	}

	public boolean isEmpty(Sign sign) {

		String lines[] = sign.getLines();
		for (int i = 0; i < 4; i++) {

			if (!lines[i].isEmpty()) {
				return false;
			}

		}
		return true;
	}
	public void updateSign(Sign sign, String line0, String line1, String line2,
			String line3) {

		sign.setLine(0, line0);
		sign.setLine(1, "§4" + line1);
		sign.setLine(2, line2);
		if (!line3.equals("")) {
			sign.setLine(3,
					"§a" + line3 + plugin.getConfig().getString("Kuerzel", "PT"));
		} else {
			sign.setLine(3, line3);
		}
		sign.update();
	}
	
	public boolean isMarkler(Sign sign) {
		if (sign instanceof Sign) {
			try {
				PreparedStatement ps = plugin.mysql
						.openConnection()
						.prepareStatement(
								"SELECT * FROM agentestate WHERE x_koord=? AND y_koord=? AND z_koord=?");
				ResultSet rs = null;

				ps.setInt(1, sign.getX());
				ps.setInt(2, sign.getY());
				ps.setInt(3, sign.getZ());
				rs = ps.executeQuery();
				if (!rs.first()) {
					return false;
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			} finally {

				plugin.mysql.closeConnection();
			}
			return true;
		}

		return false;

	}
	
	public ProtectedRegion isRegionNear(Sign sign) {
		Location loc = sign.getBlock().getLocation();
		ProtectedRegion retRegion = null;
		Integer min_area=0;
		// System.out.println(sign.getRawData());
		if (sign.getTypeId() == 63) {

			if (sign.getRawData() == 0) {
				loc.add(0, -1, -2);
			} else if (sign.getRawData() == 4) {
				loc.add(2, -1, 0);
			} else if (sign.getRawData() == 8) {
				loc.add(0, -1, 2);
			} else if (sign.getRawData() == 12) {
				loc.add(-2, -1, 0);
			}
		} else {
			if (sign.getRawData() == 3) {
				loc.add(0, -1, -2);
			} else if (sign.getRawData() == 4) {
				loc.add(2, -1, 0);
			} else if (sign.getRawData() == 2) {
				loc.add(0, -1, 2);
			} else if (sign.getRawData() == 5) {
				loc.add(-2, -1, 0);
			}
		}
		Vector v = toVector(loc);
		RegionManager manager = plugin.worldguard.getRegionManager(loc.getWorld());
		ApplicableRegionSet set = manager.getApplicableRegions(v);
		for (ProtectedRegion region : set) {

			if(min_area==0 || min_area>plugin.util.getArea(region)){
				min_area=plugin.util.getArea(region);
				retRegion=region;
			}

		}
		return retRegion;
	}
}
