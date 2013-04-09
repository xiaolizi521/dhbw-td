/*  Copyright (C) 2013 by Jan-Christoph Klie, Inc. All rights reserved.
 *  Released under the terms of the GNU General Public License version 3 or later.
 *  
 *  Contributors:
 *  Jan-Christoph Klie - Basic Structure and First Implementation
 *  Sebastian Muszytowski - Waypoint Queue and refactoring
 */

package de.dhbw.td.core.level;

import static playn.core.PlayN.assets;
import static playn.core.PlayN.json;

import java.awt.Point;
import java.util.LinkedList;
import java.util.Queue;

import playn.core.Image;
import playn.core.Json;

/**
 * 
 * @author Jan-Christoph Klie
 * @author Sebastian Muszytowski *
 */
public class SimpleLevelFactory implements ILevelFactory {
	
	private enum Direction {
		RIGHT,
		LEFT,
		UP,
		DOWN;
	}
	
	private enum ETileType {	
		
		GRID("grid.bmp"),
		WHITE("white.bmp"),
		EDGE_LEFT_BOTTOM("edge_left_bottom.bmp"),
		EDGE_LEFT_TOP("edge_left_top.bmp"),
		EDGE_RIGHT_BOTTOM("edge_right_bottom.bmp"),
		EDGE_RIGHT_TOP("edge_right_top.bmp"),
		PATH_EMPTY("path_empty.bmp"),
		PATH_HORIZONTAL("path_horizontal.bmp"),
		PATH_VERTICAL("path_vertical.bmp"),
		PATH_START("start.bmp"),
		PATH_END("finish.bmp"),
		TOWER_DUMMY("code.png");
		
		public final String resourceName;

		private static final String pathToTiles = "tiles";
		
		public static String getPathToImage(int tileID) {
			ETileType tileType = createFromTileId(tileID);
			return String.format("%s/%s", pathToTiles, tileType.resourceName);
		}
		
		private static ETileType createFromTileId(int id) {
			switch(id) {
			case 0: return GRID;
			case 1: return WHITE;
			case 2: return EDGE_LEFT_BOTTOM;
			case 3: return EDGE_LEFT_TOP;
			case 4: return EDGE_RIGHT_BOTTOM;
			case 5: return EDGE_RIGHT_TOP;
			case 6: return PATH_EMPTY;
			case 7: return PATH_HORIZONTAL;
			case 8: return PATH_VERTICAL;
			case 9: return PATH_START;
			case 10: return PATH_END;
			case 11: return TOWER_DUMMY;
			default: throw new IllegalArgumentException("No ETileType with Tile ID:" + id);
			}			
		}

		ETileType(String resourceName) {
			this.resourceName = resourceName;
		}
	}
	
	private int width;
	private int height;
	private int tilesize;
	private int startx;
	private int starty;
	private Json.Array grid;
	
	private void init(Json.Object parsedJson){
		width = parsedJson.getInt("width");
		height = parsedJson.getInt("height");
		tilesize = parsedJson.getInt("tilesize");
		startx = parsedJson.getInt("startx");
		starty = parsedJson.getInt("starty");
		grid = parsedJson.getArray("tiles");
	}
	

	@Override
	public Level loadLevel(String jsonString) {
		Json.Object configuration = json().parse(jsonString);
		return loadLevel(configuration);
	}

	@Override
	public Level loadLevel(Json.Object parsedJson) {
		init(parsedJson);

		Image[][] tileMap = loadTileMap();
		System.out.println(tileMap);
		Queue<Point> waypoints = generateWaypoints();
		
		return new Level(tileMap, waypoints, tilesize, width, height, startx, starty);
	}
	
	private Image[][] loadTileMap(){
		Image[][] tileMap = new Image[height][width];
		
		for(int row = 0; row < height; row++) {
			Json.Array gridRow = grid.getArray(row);
			
			for(int col = 0; col < width; col++) {
				int tileID = gridRow.getInt(col);
				String pathToImage = ETileType.getPathToImage(tileID);
				tileMap[row][col] = assets().getImage(pathToImage);
			}
		}
		
		return tileMap;
	}
	
	/**
	 * Method to generate waypoints from map data. It utilizes
	 * the start and end specified in the json file and calcuates the
	 * route to end. In case of unresolvable issues it returns an error.
	 * 
	 * @return Queue A queue consisting of points.
	 */
	private Queue<Point> generateWaypoints() {
		int column = startx;
		
		if(column != 0){
			throw new IllegalArgumentException("Sorry, the map should start somewhere in x=0");
		}
		
		int row = starty;
		Queue<Point> queue = new LinkedList<Point>();
		queue.add(new Point(column,row));
				
		ETileType curTile = getTileType(column, row);
		Direction direction = Direction.RIGHT;
		
		while(curTile != ETileType.PATH_END){
			switch(curTile){
			case GRID:
				throw new IllegalStateException("GRID is bad: "+column+","+row);
			case EDGE_LEFT_BOTTOM:
				if(direction == Direction.LEFT){
					queue.add(new Point(column,row));
					direction = Direction.UP;
					row--;
				}else if(direction == Direction.DOWN){
					queue.add(new Point(column,row));
					direction = Direction.RIGHT;
					column++;
				}else{
					throw new IllegalStateException("Cannot send enemy through wall at: "+column+","+row);
				}
				break;
			case EDGE_LEFT_TOP:
				if(direction == Direction.LEFT){
					queue.add(new Point(column,row));
					direction = Direction.DOWN;
					row++;
				}else if(direction == Direction.UP){
					queue.add(new Point(column,row));
					direction = Direction.RIGHT;
					column++;
				}else{
					throw new IllegalStateException("Cannot send enemy through wall at: "+column+","+row);
				}
				break;
			case EDGE_RIGHT_BOTTOM:
				if(direction == Direction.RIGHT){
					queue.add(new Point(column,row));
					direction = Direction.UP;
					row--;
				}else if(direction == Direction.DOWN){
					queue.add(new Point(column,row));
					direction = Direction.LEFT;
					column--;
				}else{
					throw new IllegalStateException("Cannot send enemy through wall at: "+column+","+row);
				}
				break; 
			case EDGE_RIGHT_TOP:
				if(direction == Direction.RIGHT){
					queue.add(new Point(column,row));
					direction = Direction.DOWN;
					row++;
				}else if(direction == Direction.UP){
					queue.add(new Point(column,row));
					direction = Direction.LEFT;
					column--;
				}else{
					throw new IllegalStateException("Cannot send enemy through wall at: "+column+","+row);
				}
				break;
				// keep the current direction in this case	
				case PATH_EMPTY:
					if(direction == Direction.LEFT){
						column--;
					}else if(direction == Direction.RIGHT){
						column++;
					}else if(direction == Direction.DOWN){
						row++;
					}else if(direction == Direction.UP){
						row--;
					}
				break;
			case PATH_HORIZONTAL:
				if(direction == Direction.LEFT){
					column--;
				}else if(direction == Direction.RIGHT){
					column++;
				}else{
					throw new IllegalStateException("Illegal waypoint detected (not left or right) at: "+column+","+row);
				}
				break;
				// we force the map to start with one step to the right
			case PATH_START:
				direction = Direction.RIGHT;
				column++;
				break;
			case PATH_VERTICAL:
				if(direction == Direction.DOWN){
					row++;
				}else if(direction == Direction.UP){
					row--;
				}else{
					throw new IllegalStateException("Illegal waypoint detected (not up or down) at: "+column+","+row);
				}
				break;
			default:
				throw new IllegalStateException("Illegal waypoint detected at: "+column+","+row);
			
			}
			
			// set tile to next tile
			curTile = getTileType(column, row);
			
			// check whether we are still in boundary. Else raise error
			if(column < 0 || row < 0 || column > width-1 || row > height-1){
				throw new IllegalStateException("Illegal map file. Waypoints leaving boundaries at: "+column+","+row);
			}
			
		}
		// here we should have reached the end if not we're unlucky
		queue.add(new Point(column,row));
		return queue;
	}
	
	private ETileType getTileType(int column,int row){
		return ETileType.createFromTileId(grid.getArray(row).getInt(column));
	}

}