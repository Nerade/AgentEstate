package de.celestialcraft.MySQLLib;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.plugin.java.JavaPlugin;

import de.celestialcraft.AgentEstate.AgentEstate;
import de.celestialcraft.AgentEstate.Estate;


public class MySQLLib {

	private Connection connect = null;
	private String user = null;
	private String password = null;
	private String database = null;
	
	public MySQLLib(String user, String password, String database){
		
		this.user=user;
		this.password=password;
		this.database=database;
	}
	
	public Connection openConnection() throws Exception{
		
		return openConnection(this.user, this.password, this.database);
	}
	
	public Connection openConnection(String user, String password,
			String database) throws Exception {

		try {
			Class.forName("com.mysql.jdbc.Driver");

			connect = DriverManager.getConnection("jdbc:mysql://localhost/"
					+ database + "?user=" + user + "&password=" + password);
			return connect;

		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw e;
		} finally {
			
		}

	}

	public Connection getConnection() throws Exception {
		if(connect==null){
			return openConnection();
		}
		return connect;
	}

	public void closeConnection() {

		this.close();

	}

	private void close() {
		try {

			if (connect != null) {
				connect.close();
			}
		} catch (Exception e) {

		}
	}
	public void reload(HashMap<String,Estate> out, AgentEstate plugin){
		out.clear();
		try {
			PreparedStatement ps = openConnection()
				.prepareStatement(
						"SELECT `RegionID` FROM `agentestate` WHERE 1");
			ResultSet rs = ps.executeQuery();
			while(rs.next()){
				out.put(rs.getString("RegionID"), new Estate(rs.getString("RegionID"), plugin));
			}
			
		} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		} finally {
		closeConnection();
		}
	}
	
	public void save(HashMap<String,Estate> in){
		Collection<Estate> col = in.values();
		for(Estate el:col){
				
			try {
				PreparedStatement ps = openConnection()
						.prepareStatement(
								"UPDATE `agentestate` SET `Owner` = ? `Seller` = ? `Renter` = ? `Price` = ? `Rent` = ? `Period` = ? `Area` = ? `world` = ?  x_koord=?  y_koord=?  z_koord=? WHERE `RegionID` = ?");
				
				ps.setString(1, el.getOwner());
				ps.setString(2, el.getSeller());
				ps.setString(3, el.getRenter());
				ps.setInt(4, el.getPrice());
				ps.setInt(5, el.getRent());
				ps.setInt(6, el.getPeriod());
				ps.setInt(7, el.getArea());
				ps.setString(8, el.getWorld());
				ps.setInt(9, el.getLocation().getBlockX());
				ps.setInt(10, el.getLocation().getBlockY());
				ps.setInt(11, el.getLocation().getBlockZ());
				ps.setString(12, el.getRegionId());
				ps.execute();

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
			closeConnection();
			}
		}	
	}
}
