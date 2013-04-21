package de.celestialcraft.util;

import org.bukkit.entity.Player;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import de.celestialcraft.AgentEstate.AgentEstate;
import de.celestialcraft.AgentEstate.Estate;

public class Util {
	
	private AgentEstate plugin=null;
	
	public Util(AgentEstate instance){
		
		this.plugin=instance;
		
	}
	
	public int getArea(ProtectedRegion region) {

		int x = region.getMaximumPoint().getBlockX()
				- region.getMinimumPoint().getBlockX() + 1;
		int z = region.getMaximumPoint().getBlockZ()
				- region.getMinimumPoint().getBlockZ() + 1;

		return x * z;
	}
	
	public boolean regionChangeOwner(String region_Id, Player p) {

		DefaultDomain domain = new DefaultDomain();
		DefaultDomain domain2 = new DefaultDomain();
		domain.addPlayer(p.getName());

		Estate activeEstate = new Estate(region_Id,plugin);
		ProtectedRegion region = activeEstate.getRegion();

		region.setMembers(domain2);
		region.setOwners(domain);
		
		try {
			plugin.worldguard.getRegionManager(
					plugin.getServer().getWorld(activeEstate.getWorld())).save();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean regionChangeMember(String region_Id, Player p) {

		DefaultDomain domain = new DefaultDomain();
		domain.addPlayer(p.getName());

		Estate activeEstate = new Estate(region_Id,plugin);
		ProtectedRegion region = activeEstate.getRegion();
		
		region.setMembers(domain);
		
		try {
			plugin.worldguard.getRegionManager(
					plugin.getServer().getWorld(activeEstate.getWorld())).save();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	public boolean regionChangeMember(String region_Id) {

		DefaultDomain domain = new DefaultDomain();

		Estate activeEstate = new Estate(region_Id,plugin);
		ProtectedRegion region = activeEstate.getRegion();
		
		region.setMembers(domain);
		
		try {
			plugin.worldguard.getRegionManager(
					plugin.getServer().getWorld(activeEstate.getWorld())).save();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	public boolean regionChangeOwner(String region_Id) {

		DefaultDomain domain = new DefaultDomain();
		
		Estate activeEstate = new Estate(region_Id,plugin);
		ProtectedRegion region = activeEstate.getRegion();
		
		region.setMembers(domain);
		region.setOwners(domain);

		try {
			plugin.worldguard.getRegionManager(
					plugin.getServer().getWorld(activeEstate.getWorld())).save();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
