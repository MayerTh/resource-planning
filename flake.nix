{
  description = "resource-planning";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
      in
      {
        packages.default = pkgs.maven.buildMavenPackage {
          pname = "resource-planning";
          version = "0.1.0";
          src = ./.;

          mvnHash = "sha256-DOfxXfLoT3etsmwrJMxyNnWvUiJ7dXCOnIX64oFNqL4=";
          buildOffline = true;

          installPhase = ''
            runHook preInstall
            mkdir -p $out/share/resource-planning $out/bin
            install -Dm444 target/resource-planning.jar $out/share/resource-planning/resource-planning.jar
            cat > $out/bin/resource-planning <<EOF
            #!/usr/bin/env bash
            exec ${pkgs.jdk}/bin/java -jar $out/share/resource-planning/resource-planning.jar
            EOF
            chmod +x $out/bin/resource-planning
            runHook postInstall
          '';
        };

        apps.default = {
          type = "app";
          program = "${self.packages.${system}.default}/bin/resource-planning";
        };

        devShells.default = pkgs.mkShell {
          packages = [ pkgs.jdk pkgs.maven ];
        };
      });
}
