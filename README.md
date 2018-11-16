
# react-native-spatial

## Getting started

`$ npm install react-native-spatial --save`

### Mostly automatic installation

`$ react-native link react-native-spatial`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-spatial` and add `RNSpatial.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNSpatial.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.spatial.RNSpatialPackage;` to the imports at the top of the file
  - Add `new RNSpatialPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-spatial'
  	project(':react-native-spatial').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-spatial/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-spatial')
  	```

## Usage
```javascript
import RNSpatial from 'react-native-spatial';

const runExample = async () => {
    console.log(RNSpatial);

        await RNSpatial.connect('testingDB');
        await RNSpatial.createTable({
            tableName: 'test_geom',
            columns: [
                {
                    name: 'id',
                    type: 'INTEGER',
                    constraints: [
                        'PRIMARY KEY',
                        'NOT NULL'
                    ]
                },
                {
                    name: 'name',
                    type: 'TEXT',
                    constraints: [
                        'NOT NULL'
                    ]
                },
                {
                    name: 'measured_value',
                    type: 'DOUBLE',
                    constraints: [
                        'NOT NULL'
                    ]
                }
            ]
        });
        await RNSpatial.executeQuery("SELECT AddGeometryColumn('test_geom', 'the_geom', 4326, 'POINT', 'XY');");
        await RNSpatial.executeQuery("INSERT INTO test_geom (id, name, measured_value, the_geom) VALUES (10, 'tenth point',             10.123456789, GeomFromText ('POINT(10.01 10.02)', 4326));");
        RNSpatial.executeQuery("SELECT * FROM test_geom;")
            .then(response => {
                console.log(response);
                // don't forget to close connection
                RNSpatial.close();
            })
};

runExample();

```
  
