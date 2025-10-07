import {NativeModules, Platform} from 'react-native';

const {NavigationBarColor} = NativeModules;

const changeNavigationBarColor = (
  color = String,
  light = false,
  animated = true,
) => {
  if (Platform.OS === 'android') {
    const LightNav = light ? true : false;
    return NavigationBarColor.changeNavigationBarColor(color, LightNav, animated);
  }
  return Promise.resolve();
};
const hideNavigationBar = () => {
  if (Platform.OS === 'android') {
    return NavigationBarColor.hideNavigationBar();
  }
  return Promise.resolve(false);
};
const showNavigationBar = () => {
  if (Platform.OS === 'android') {
    return NavigationBarColor.showNavigationBar();
  }
  return Promise.resolve(false);
};
const isNavigationBarVisible = () => {
    if (Platform.OS === 'android') {
     return NavigationBarColor.isNavigationBarVisible();
    } else {
     return Promise.resolve({ isVisible: false, navigationBarHeight: 0 });
    }
};

export {changeNavigationBarColor, hideNavigationBar, showNavigationBar, isNavigationBarVisible};
