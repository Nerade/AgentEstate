package de.celestialcraft.AgentEstate;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import com.iCo6.system.Holdings;
import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.data.DataException;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.schematic.MCEditSchematicFormat;
import com.sk89q.worldedit.schematic.SchematicFormat;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class Estate { // Klasse, die ein Grundstück repräsentiert

	private int price, area, rent, period;
	private boolean registered, forRent;
	private String region_id, owner, seller, world, renter;
	private Timestamp expires;
	private Location region_loc;
	private Sign makler;
	private AgentEstate plugin;
	private ProtectedRegion region;

	public Estate(String region_id, AgentEstate plugin) { // Hole Daten aus der
															// Datenbank
		this.plugin = plugin;
		this.region_id = region_id;
		this.registered = false;
		this.forRent = false;

		try {
			PreparedStatement ps = plugin.mysql.openConnection()
					.prepareStatement(
							"SELECT * FROM agentestate WHERE RegionID=?");
			ps.setString(1, this.region_id);
			ResultSet rs = ps.executeQuery();
			if ((rs != null) && (rs.first() == true)) {
				this.price = rs.getInt("Price");
				this.rent = rs.getInt("Rent");
				this.area = rs.getInt("Area");
				this.owner = rs.getString("Owner");
				this.seller = rs.getString("Seller");
				this.renter = rs.getString("Renter");
				this.world = rs.getString("World");
				this.period = rs.getInt("Period");
				this.expires = rs.getTimestamp("Expires");
				this.registered = true;
				this.region_loc = new Location(plugin.getServer().getWorld(
						world), rs.getInt("x_koord"), rs.getInt("y_koord"),
						rs.getInt("z_koord"));
				this.makler = (Sign) plugin.getServer().getWorld(world)
						.getBlockAt(this.region_loc).getState();
				this.region = plugin.worldguard.getRegionManager(
						plugin.getServer().getWorld(world))
						.getRegion(region_id);
				if (this.rent != 0) {
					this.forRent = true;
				}
				this.registered = true;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			plugin.mysql.closeConnection();
		}

	}

	public Estate(Sign sign, AgentEstate plugin) {

		this(sign.getLine(1).substring(2), plugin);
		this.makler = sign;
	}

	public boolean create(Sign sign) {
		return create(sign, false);
	}

	public boolean create(Sign sign, boolean rentable) { // create: Methode zur
															// Definition eines
															// neuen Grundstücks
		this.makler = sign;
		this.region_loc = this.makler.getLocation();
		this.world = this.makler.getWorld().getName();
		this.forRent = rentable;
		if (this.registered == true) {
			System.out.println("Bereits registriert!");
			return false;
		}
		if (area == 0) {
			World world = sign.getWorld();
			this.area = plugin.util.getArea(plugin.worldguard.getRegionManager(
					world).getRegion(region_id));
		}

		// System.out.println("Trage GS in DB ein...");
		try {
			PreparedStatement ps = plugin.mysql
					.openConnection()
					.prepareStatement(
							"INSERT INTO `agentestate` (`RegionID` ,`Owner` ,`Seller` ,`Price` ,`Rent` ,`Period` ,`Area` ,`World` , `x_koord` ,`y_koord` ,`z_koord`)VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

			ps.setString(1, this.region_id);
			ps.setString(2, this.owner);
			ps.setString(3, this.seller);
			ps.setInt(4, this.price);
			ps.setInt(5, this.rent);
			ps.setInt(6, this.getPeriod());
			ps.setInt(7, this.area);
			ps.setString(8, this.world);
			ps.setInt(9, (int) region_loc.getX());
			ps.setInt(10, (int) region_loc.getY());
			ps.setInt(11, (int) region_loc.getZ());
			ps.executeUpdate();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} finally {
			plugin.mysql.closeConnection();
		}
		this.registered = true;
		if (!rentable) {
			plugin.sign_util.updateSign(sign, "Zu verkaufen:", this.region_id,
					plugin.getConfig().getString("Bank", "Bank"),
					String.valueOf(this.price));
		} else {
			plugin.sign_util.updateSign(sign, "Zu vermieten:", this.region_id,
					plugin.getConfig().getString("Bank", "Bank"),
					String.valueOf(this.rent));
		}
		plugin.estates.put(this.region_id, this);
		if((plugin.getConfig().getBoolean("Save_Rent") && rentable == true) || (plugin.getConfig().getBoolean("Save_Buy") && rentable == false)){
			saveState();
		}
		return true;
	}

	public boolean createRentable(Sign sign) {
		if (this.getPeriod() == 0) {
			this.setPeriod(plugin.getConfig().getInt("Default_Period"));
		}
		return create(sign, true);
	}

	public boolean repair(Sign sign, String owner) { // repair: Repariere das
														// aktuelle Grundstück
														// in der Datenbank

		this.makler = sign;
		this.region_loc = this.makler.getLocation();
		this.world = this.makler.getWorld().getName();
		if (this.registered == true) {
			System.out.println("Bereits registriert!");
			return false;
		}
		if (area == 0) {
			World world = sign.getWorld();
			this.area = plugin.util.getArea(plugin.worldguard.getRegionManager(
					world).getRegion(region_id));
		}

		// System.out.println("Trage GS in DB ein...");
		try {
			PreparedStatement ps = plugin.mysql
					.openConnection()
					.prepareStatement(
							"INSERT INTO `agentestate` (`RegionID` ,`Owner` ,`Seller` ,`Price` ,`Area` ,`World` , `x_koord` ,`y_koord` ,`z_koord`)VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");

			ps.setString(1, this.region_id);
			ps.setString(2, this.owner);
			ps.setString(3, this.seller);
			ps.setInt(4, this.price);
			ps.setInt(5, this.area);
			ps.setString(6, this.world);
			ps.setInt(7, (int) region_loc.getX());
			ps.setInt(8, (int) region_loc.getY());
			ps.setInt(9, (int) region_loc.getZ());
			ps.executeUpdate();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} finally {
			plugin.mysql.closeConnection();
		}
		this.registered = true;

		plugin.sign_util.updateSign(sign, "", this.region_id, owner, "");
		return true;
	}

	public boolean remove() { // remove: Lösche das Grundstück
		if (makler.getLine(1).length() >= 3) {
			try {
				PreparedStatement ps = plugin.mysql
						.openConnection()
						.prepareStatement(
								"DELETE FROM `agentestate` WHERE `RegionID` = ?");

				ps.setString(1, makler.getLine(1).substring(2));
				ps.execute();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
			deleteState();
			plugin.estates.remove(makler.getLine(1).substring(2));
			return true;
		} else {
			return false;
		}

	}

	public boolean sell() { // sell: Routine zum Verkauf des Grundstückes

		try {
			PreparedStatement ps = plugin.mysql
					.openConnection()
					.prepareStatement(
							"UPDATE `agentestate` SET `Owner` = ?, `Seller` = ?, `Price` = ? WHERE world=? AND x_koord=? AND y_koord=? AND z_koord=?");
			ps.setString(1, "");
			ps.setString(2, this.owner);
			ps.setInt(3, this.price);
			ps.setString(4, this.world);
			ps.setInt(5, (int) this.region_loc.getX());
			ps.setInt(6, (int) this.region_loc.getY());
			ps.setInt(7, (int) this.region_loc.getZ());
			ps.execute();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} finally {

			plugin.mysql.closeConnection();
		}

		this.seller = this.owner;
		this.owner = "";
		plugin.util.regionChangeOwner(this.getRegionId());
		plugin.file_util.updateStats(this.getSeller(), true);
		plugin.sign_util.updateSign(this.makler, "Zu verkaufen:",
				this.getRegionId(), this.getSeller(),
				String.valueOf(this.getPrice()));
		restoreState();
		return true;
	}

	public boolean buy(Player player) { // buy: Routine zum Kauf

		// ############# Wenn was im Preis steht #################
		if (getPrice() >= 0) {

			// int temp = (int) (player.getLocation().getX() * 4);
			// temp += player.getLocation().getZ();
			// if ((plugin.confirm.get(player.getName()) == null) ||
			// (plugin.confirm.get(player.getName()) != temp)) {
			// player.sendMessage("Klicke ein weiteres mal zur Bestätigung...");
			// plugin.confirm.put(player.getName(), temp);
			// return false;
			//
			// } else {

			Holdings balance_buy = new Holdings(player.getName());
			Holdings balance_sell = new Holdings(this.getSeller());

			int price = getPrice();
			if (!balance_buy.hasEnough(price)
					&& !(getSeller().equals(player.getName()))) {
				player.sendMessage(ChatColor.RED + "Du hast nicht genug Geld!");
				balance_sell = null;
				balance_buy = null;

				return false;
			}

			// System.out.println(price);
			balance_sell.add(price);
			balance_buy.subtract(price);

			balance_sell = null;
			balance_buy = null;
			try {

				PreparedStatement ps = plugin.mysql
						.openConnection()
						.prepareStatement(
								"UPDATE `agentestate` SET `Owner` = ?, `Seller` = ?, `Price` = ? WHERE world=? AND x_koord=? AND y_koord=? AND z_koord=?");
				ps.setString(1, player.getName());
				ps.setString(2, "");
				ps.setInt(3, -1);
				ps.setString(4, this.getWorld());
				ps.setInt(5, this.getLocation().getBlockX());
				ps.setInt(6, this.getLocation().getBlockY());
				ps.setInt(7, this.getLocation().getBlockZ());
				ps.executeUpdate();

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				player.sendMessage(ChatColor.RED + "Fehler beim Kauf!");
				return false;
			} finally {
				plugin.mysql.closeConnection();
			}
			plugin.gs_start.remove(player.getName());
			plugin.util.regionChangeOwner(this.getRegionId(), player);
			plugin.file_util.updateStats(player.getName(), false);
			plugin.sign_util.updateSign(
					(Sign) plugin.getServer().getWorld(this.getWorld())
							.getBlockAt(this.getLocation()).getState(), "",
					this.getRegionId(), player.getName(), "");
			return true;
		} else {
			return false;
		}
		// }

	}

	public boolean rent(Player player) { // rent: Routine zum Mieten

		Long expires = 0L;
		Holdings balance = new Holdings(player.getName());
		if (balance.hasEnough(this.getRent())) {

			balance.subtract(this.getRent());

			try {

				Connection con = plugin.mysql.openConnection();

				PreparedStatement ps = con
						.prepareStatement("SELECT `Expires`,`Period` FROM `agentestate` WHERE world=? AND x_koord=? AND y_koord=? AND z_koord=?");

				ps.setString(1, this.getWorld());
				ps.setInt(2, this.getLocation().getBlockX());
				ps.setInt(3, this.getLocation().getBlockY());
				ps.setInt(4, this.getLocation().getBlockZ());

				ResultSet rs = ps.executeQuery();

				while (rs.next()) {
					if (rs.getTimestamp("Expires") == null
							|| rs.getTimestamp("Expires").before(
									new Timestamp(System.currentTimeMillis()))) {
						expires = System.currentTimeMillis();
					} else {
						expires = rs.getTimestamp("Expires").getTime();
					}
					this.period = rs.getInt("Period");
				}
				// System.out.println(this.getPeriod());
				ps = con.prepareStatement("UPDATE `agentestate` SET `Renter`= ? , `Expires`= ? WHERE world=? AND x_koord=? AND y_koord=? AND z_koord=?");
				ps.setString(1, player.getName());
				ps.setTimestamp(2, new Timestamp(expires + this.getPeriod()
						* 1000 * 3600 * 24));
				ps.setString(3, this.getWorld());
				ps.setInt(4, this.getLocation().getBlockX());
				ps.setInt(5, this.getLocation().getBlockY());
				ps.setInt(6, this.getLocation().getBlockZ());
				ps.executeUpdate();

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				player.sendMessage(ChatColor.RED + "Fehler beim Mieten!");
				return false;
			} finally {
				plugin.mysql.closeConnection();
			}
			// System.out.println(new Timestamp(expires + this.getPeriod()
			// * 1000 * 3600 * 24));
			plugin.util.regionChangeMember(this.getRegionId(), player);
			plugin.file_util.updateStats(player.getName(), false);
			plugin.sign_util.updateSign(
					(Sign) plugin.getServer().getWorld(this.getWorld())
							.getBlockAt(this.getLocation()).getState(),
					"Vermietet!", this.getRegionId(), player.getName(), "");
			return true;
		} else {
			player.sendMessage(ChatColor.RED + "Du hast nicht genug Geld!");
		}
		return false;
	}

	public boolean release() { // release: Enteigne Mieter

		try {
			PreparedStatement ps = plugin.mysql
					.openConnection()
					.prepareStatement(
							"UPDATE `agentestate` SET `Renter` = ? WHERE world=? AND x_koord=? AND y_koord=? AND z_koord=?");
			ps.setString(1, this.renter);
			ps.setString(2, this.world);
			ps.setInt(3, (int) this.region_loc.getX());
			ps.setInt(4, (int) this.region_loc.getY());
			ps.setInt(5, (int) this.region_loc.getZ());
			ps.execute();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} finally {

			plugin.mysql.closeConnection();
		}
		plugin.util.regionChangeMember(this.getRegionId());
		plugin.file_util.updateStats(this.getRenter(), true);
		plugin.sign_util.updateSign(
				(Sign) plugin.getServer().getWorld(this.getWorld())
						.getBlockAt(this.getLocation()).getState(),
				"Zu vermieten:", this.getRegionId(), "Bank",
				String.valueOf(this.rent));
		restoreState();
		return true;
	}

	private void deleteState() {
		
		File sch = new File(plugin.getDataFolder()+ File.separator + "schematics" + File.separator + this.getRegionId());
		sch.delete();
	}
	
	private void restoreState(){
		
		File sch = new File(plugin.getDataFolder()+ File.separator + "schematics" + File.separator + this.getRegionId()+ ".sch");
		if(!sch.exists()){
			return;
		}
		SchematicFormat format = SchematicFormat.getFormat("MCEdit");
		EditSession es = new EditSession(new BukkitWorld(plugin.getServer().getWorld(this.getWorld())), 1000000);
		CuboidClipboard cubo;
		try {
			cubo = format.load(sch);
			cubo.paste(es, new Vector(this.getLocation().getBlockX(),this.getLocation().getBlockY(),this.getLocation().getBlockZ()), true);
		} catch (IOException | DataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MaxChangedBlocksException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private boolean saveState() {

		ProtectedRegion region = plugin.worldguard.getRegionManager(
				plugin.getServer().getWorld(this.getWorld())).getRegion(
				this.getRegionId());
		
		Vector min = region.getMinimumPoint();
		Vector max = region.getMaximumPoint();
		
        CuboidClipboard cubo = new CuboidClipboard(
                max.subtract(min).add(new Vector(1, 1, 1)),
                min);

		SchematicFormat format;
		if (SchematicFormat.getFormats().size() == 1) {
			format = SchematicFormat.getFormats().iterator().next();
		} else {
			System.out
					.println("More than one schematic format is available. Please provide the desired format");
			return false;
		}
		File sch = new File(plugin.getDataFolder()+ File.separator + "schematics" + File.separator + this.getRegionId() + ".sch");
		System.out.println(sch);
		System.out.println(cubo);
		try {
			format.save(cubo, sch);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return true;
	}

	public boolean isExpired() { // isExpired: ist Miete ausgelaufen?

		if (this.expires.before(new Timestamp(System.currentTimeMillis()))) {
			return true;
		} else {
			return false;
		}
	}

	public void setPrice(Integer price) {
		this.price = price;
	}

	public Integer getPrice() {
		return this.price;
	}

	public int getArea() {
		return this.getArea();
	}

	public String getOwner() {
		return this.owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getSeller() {
		return this.seller;
	}

	public void setSeller(String seller) {
		this.seller = seller;
	}

	public Location getLocation() {
		return this.region_loc;
	}

	public String getRegionId() {
		return this.region_id;
	}

	public String getWorld() {
		return this.world;
	}

	public ProtectedRegion getRegion() {
		return this.region;
	}

	public boolean isRentable() {
		return this.forRent;
	}

	public void setRent(Integer rent) {
		this.rent = rent;
	}

	public Integer getRent() {
		return this.rent;
	}

	public String getRenter() {
		return this.renter;
	}

	public void setPeriod(Integer period) {
		this.period = period;
	}

	public Integer getPeriod() {
		return this.period;
	}

	public Timestamp getExpires() {
		return this.expires;
	}

}
