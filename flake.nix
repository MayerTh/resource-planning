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
        packages.default = pkgs.stdenv.mkDerivation {
          pname = "resource-planning";
          version = "0.1.0";
          src = ./.;
          nativeBuildInputs = [ pkgs.jdk ];

          buildPhase = ''
            javac -d out src/Main.java
          '';

          installPhase = ''
            mkdir -p $out/share/resource-planning $out/bin
            cp -r out/* $out/share/resource-planning/
            cat > $out/bin/resource-planning <<EOF
            #!/usr/bin/env bash
            exec ${pkgs.jdk}/bin/java -cp $out/share/resource-planning Main
            EOF
            chmod +x $out/bin/resource-planning
          '';
        };

        apps.default = {
          type = "app";
          program = "${self.packages.${system}.default}/bin/resource-planning";
        };

        devShells.default = pkgs.mkShell {
          packages = [ pkgs.jdk ];
        };
      });
}
