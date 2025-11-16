Pod::Spec.new do |s|
  s.name         = "react-native-uhf-tag"
  s.version      = "1.2.0"
  s.summary      = "Modern React Native library for UHF RFID tag reading with Turbo Module support"
  s.description  = <<-DESC
                  React Native library for UHF RFID tag reading with Chainway R6 and compatible devices.
                  Built with Turbo Module architecture for improved performance.
                  DESC
  s.homepage     = "https://github.com/yourusername/react-native-uhf-tag"
  s.license      = "MIT"
  s.author       = { "Your Name" => "your.email@example.com" }
  s.platforms    = { :ios => "11.0" }
  s.source       = { :git => "https://github.com/yourusername/react-native-uhf-tag.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,m,mm,swift}"
  
  s.dependency "React-Core"

  # Don't install the dependencies when we run `pod install` in the old architecture.
  if ENV['RCT_NEW_ARCH_ENABLED'] == '1' then
    s.compiler_flags = folly_compiler_flags + " -DRCT_NEW_ARCH_ENABLED=1"
    s.pod_target_xcconfig    = {
        "HEADER_SEARCH_PATHS" => "\"$(PODS_ROOT)/boost\"",
        "OTHER_CPLUSPLUSFLAGS" => "-DFOLLY_NO_CONFIG -DFOLLY_MOBILE=1 -DFOLLY_USE_LIBCPP=1",
        "CLANG_CXX_LANGUAGE_STANDARD" => "c++17"
    }
    s.dependency "React-RCTFabric"
    s.dependency "React-Codegen"
    s.dependency "RCT-Folly"
    s.dependency "RCTRequired"
    s.dependency "RCTTypeSafety"
    s.dependency "ReactCommon/turbomodule/core"
  end
end
