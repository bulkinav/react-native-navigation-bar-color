declare module "react-native-navigation-bar-color" {
    function changeNavigationBarColor(color: string, light?: boolean, animated?: boolean): Promise<void>;
    function hideNavigationBar(): boolean;
    function showNavigationBar(): boolean;
    function isNavigationBarVisible(): Promise<{ isVisible: boolean; navigationBarHeight: number; }>;

    export default changeNavigationBarColor;
    export { hideNavigationBar, showNavigationBar, isNavigationBarVisible };
}
