
package com.reactlibrary;

import android.os.Environment;
import android.widget.Toast;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

import jsqlite.Constants;
import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;

public class RNSpatialModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;
  
  private static Database db;
  private static boolean isConnected = false;
  private static String docDir;

  public RNSpatialModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNSpatial";
  }


  @Override
  public Map<String, Object> getConstants() {

        final Map<String, Object> constants = new HashMap<>();

      return constants;
  }

  @ReactMethod
  public void show(String message, int duration) {
    Toast.makeText(getReactApplicationContext(), Environment.getDataDirectory().getAbsolutePath(), duration).show();
  }

  @ReactMethod
    public void connect(String dbName, Promise promise) {
       try {
			dbName = dbName.trim();
			if (dbName.isEmpty()) {
				promise.reject("DBName can't be empty!", new NullPointerException());
				return;
			}
			dbName = dbName.endsWith(".sqlite") ? dbName : dbName.concat(".sqlite");
			docDir = getReactApplicationContext().getExternalFilesDir(null).getAbsolutePath();
			WritableMap map = Arguments.createMap();
			db = new Database();
			db.open(docDir + "/" + dbName, Constants.SQLITE_OPEN_READWRITE | Constants.SQLITE_OPEN_CREATE);
//			Check spatial initialized
           boolean isSpatial = false;
			try {
				isSpatial = db.prepare("select count(1) from spatial_ref_sys limit 1").step();
			} catch (Exception e) {
				if (e.getMessage().trim().startsWith("no such table: spatial_ref_sys")) {
					db.exec("SELECT InitSpatialMetaData(1)", null);
				}
			}
			isConnected = true;
			map.putBoolean("isConnected", isConnected);
			map.putString("spatialStatus", isSpatial ? "Already Spatial" : "Spatial Added");
			promise.resolve(map);
       } catch (Exception e) {
           promise.reject(e.getMessage(), e);
       }
  }

  @ReactMethod
    public void close(Promise promise) {
      try {
          db.close();
          isConnected = false;
          WritableMap map = Arguments.createMap();
          map.putBoolean("isConnected", isConnected);
          promise.resolve(map);
      } catch (Exception e) {
          promise.reject(e.getMessage(), e);
      }
  }

  @ReactMethod
    public void executeQuery(String query, ReadableMap params, Promise promise) {
      try {
          Stmt stmt = db.prepare(query);

          for (int i=0; i < stmt.bind_parameter_count(); i++) {
              String pName = stmt.bind_parameter_name(i+1).substring(1);
              if (!params.hasKey(pName)) {
                  promise.reject("Value not specified for "+ pName, "Value not specified for "+ pName);
                  return;
              }
              switch (params.getType(pName)) {
                  case Number:
                      stmt.bind(stmt.bind_parameter_index("@"+pName), params.getDouble(pName));
                      break;
                  case String:
                      stmt.bind(stmt.bind_parameter_index("@"+pName), params.getString(pName));
                      break;
                  case Null:
                      stmt.bind(stmt.bind_parameter_index("@"+pName));
                      break;
                  default:
                      stmt.bind(stmt.bind_parameter_index("@"+pName), params.getString(pName));
                      break;
              }
          }

          int rowCount = 0;
          int colCount = 0;
          WritableMap result = Arguments.createMap();
          WritableArray rows = Arguments.createArray();
          while (stmt.step()) {
			  rowCount++;
              if (colCount == 0)
                  colCount = stmt.column_count();
              WritableMap row = Arguments.createMap();
              for (int i = 0; i < colCount; i++) {
                  switch (stmt.column_type(i)) {
                      case Constants.SQLITE3_TEXT:
                          row.putString(stmt.column_name(i).toLowerCase(), stmt.column_string(i));
                          break;
                      case Constants.SQLITE_INTEGER:
                          row.putInt(stmt.column_name(i).toLowerCase(), (int) stmt.column_long(i));
                          break;
                      case Constants.SQLITE_FLOAT:
                          row.putDouble(stmt.column_name(i).toLowerCase(), stmt.column_double(i));
                          break;
                      case Constants.SQLITE_NULL:
                          row.putNull(stmt.column_name(i).toLowerCase());
                          break;
                      default:
                          row.putString(stmt.column_name(i).toLowerCase(), stmt.column_string(i));
                          break;
                  }
              }
              rows.pushMap(row);
          }
          result.putInt("rows", rowCount);
          result.putInt("cols", colCount);
          result.putArray("data", rows);
          promise.resolve(result);
          stmt.clear_bindings();
      } catch (Exception e) {
          promise.reject(e.getMessage(), e);
      }
  }

  @ReactMethod
    public void executeUpdate(String query, ReadableMap params, Promise promise) {
      try {
          Stmt stmt = db.prepare(query);

          for (int i=0; i < stmt.bind_parameter_count(); i++) {
              String pName = stmt.bind_parameter_name(i+1).substring(1);
              if (!params.hasKey(pName)) {
                  promise.reject("Error", "Value not specified for "+ pName);
                  return;
              }
              switch (params.getType(pName)) {
                  case Number:
                      stmt.bind(stmt.bind_parameter_index("@"+pName), params.getDouble(pName));
                      break;
                  case String:
                      stmt.bind(stmt.bind_parameter_index("@"+pName), params.getString(pName));
                      break;
                  case Null:
                      stmt.bind(stmt.bind_parameter_index("@"+pName));
                      break;
                  default:
                      stmt.bind(stmt.bind_parameter_index("@"+pName), params.getString(pName));
                      break;
              }
          }
          WritableMap result = Arguments.createMap();
          if (stmt.step()) {
              result.putInt("count", stmt.column_count());
              if (stmt.column_count() > 0){
                  result.putString("data", stmt.column_string(0));
              }
          }
          stmt.clear_bindings();
          promise.resolve(result);
      } catch (Exception e) {
          promise.reject(e.getMessage(), e);
      }
  }

  @ReactMethod
    public void createTable(ReadableMap params, Promise promise) {
      try {
          String validation = validateTableStructure(params);
          if (!validation.equals("Valid")) {
              promise.reject(validation, validation);
              return;
          }
          StringBuilder sb = new StringBuilder("CREATE TABLE");
          sb.append(" ").append(params.getString("tableName")).append(" (");
          ReadableArray columns = params.getArray("columns");
          for (int i = 0; i < columns.size(); i++) {
              ReadableMap col = columns.getMap(i);
              validation = validateColumnStructure(col);
              if (!validation.equals("Valid")) {
                  promise.reject(validation, validation);
                  return;
              }
              sb.append(" ").append(col.getString("name"));
              sb.append(" ").append(col.getString("type"));
              if (col.hasKey("constraints")) {
                  ReadableArray constraints = col.getArray("constraints");
                  for (int j = 0; j < constraints.size(); j++) {
                      sb.append(" ").append(constraints.getString(j));
                  }
              }
              if (i != columns.size()-1)
              sb.append(",");
          }

          sb.append(");");
          db.exec(sb.toString(), null);
          WritableMap result = Arguments.createMap();
          result.putBoolean("success", true);
          result.putString("query", sb.toString());
          promise.resolve(result);
      } catch (Exception e) {
          promise.reject(e.getMessage(), e);
      }
  }

    private String validateColumnStructure(ReadableMap col) {
        if (!col.hasKey("name") ||
                !col.hasKey("type")) {
            return "column must've name and type";
        }
        if (col.getType("name") != ReadableType.String) {
            return "column name must be a valid string";
        }
        if (col.getType("type") != ReadableType.String) {
            return "column type must be a valid string";
        }
        if (col.hasKey("constraints")) {
            if (col.getType("constraints") != ReadableType.Array) {
                return "column constraints must be array of string(s)";
            } else {
                ReadableArray constraints = col.getArray("constraints");
                for (int i = 0; i < constraints.size(); i++) {
                    if (constraints.getType(i) != ReadableType.String) {
                        return "column constraints must be array of string(s)";
                    }
                }
            }
        }
        return "Valid";
    }

    private String validateTableStructure(ReadableMap params) {

        if (!params.hasKey("tableName")
                || params.getString("tableName").trim().isEmpty()
                || params.isNull("tableName")) {
            return "tableName cannot be empty or null";
        }
        if (params.getType("columns") != ReadableType.Array) {
            return "columns must be an array of json objects";
        } else if(params.getArray("columns").size() > 0){
            if (params.getArray("columns").getType(0) != ReadableType.Map) {
                return "columns must be of type json";
            }
        }
        return "Valid";
    }
}