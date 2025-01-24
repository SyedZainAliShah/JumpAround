This project is an advanced take on the classic Pong game, built in Java using JOGL (Java OpenGL). It includes modern graphical enhancements such as Phong and GGX microfacet shading to demonstrate realistic lighting effects.

Features
Realistic Shading: Implements GGX microfacet BRDF for enhanced visual realism.
Dynamic Controls: Adjust metallic and roughness properties on-the-fly using keyboard inputs.
Power-Ups: Gameplay enhanced with power-ups for added challenge and fun.
Light Control: Change light direction dynamically or follow the ball.
Controls
Gameplay:

Player 1: W (up), S (down)
Player 2: P (up), L (down)
SPACE: Start/Restart game
Lighting:

0-4: Change light direction
5: Set metallic = 0
6: Set metallic = 1
7: Set roughness = 0.1
8: Set roughness = 0.2
Requirements
Java 8 or higher
JOGL library
Installation
Clone the repository:
bash
Copy
Edit
git clone https://github.com/yourusername/PongPhong.git
Open the project in your favorite IDE.
Ensure JOGL is properly configured.
Run the PongGame class to start playing!
