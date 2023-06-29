package com.tsnsched.core.network;
//TSNsched uses the Z3 theorem solver to generate traffic schedules for Time Sensitive Networking (TSN)
//
//    TSNsched is licensed under the GNU GPL version 2 or later.
//    
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <https://www.gnu.org/licenses/>.


public enum ResetMethod {
	PORTHARDRESET, // Resets everything from the port
	PORTCARRYONFRAGMENTS, // Carry
	PORTCARRYONCYCLE,
	PORTSOFTRESET
	// Port reset methods idea: freeze fragments, freeze cycle, freeze both, carry on both (soft reset)

}
