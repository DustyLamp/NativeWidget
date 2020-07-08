#import "NativeWidgetPlugin.h"
#if __has_include(<nativewidget/nativewidget-Swift.h>)
#import <nativewidget/nativewidget-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "nativewidget-Swift.h"
#endif

@implementation NativeWidgetPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftNativeWidgetPlugin registerWithRegistrar:registrar];
}
@end
