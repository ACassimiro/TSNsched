package com.tsnsched.core.sched2netconf;

import com.tsnsched.core.*;
//TSNsched uses the Z3 theorem solver to generate traffic schedules for Time Sensitive Networking (TSN)
//
//    TSNsched is licensed under the GNU GPL version 2 or later.
//    
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <https://www.gnu.org/licenses/>.


/** Utility class to store queue ID, slotStart and slot Duration. */
public class Triple implements Comparable<Triple> {
  private Integer queueID;
  private Long slotStart;
  private Long slotDuration;

  public Triple(Integer queueID, Long slotStart, Long slotDuration) {
    this.queueID = queueID;
    this.slotStart = slotStart;
    this.slotDuration = slotDuration;
  }

  public Integer getQueueID() {
    return queueID;
  }

  public Long getSlotStart() {
    return slotStart;
  }

  public Long getSlotDuration() {
    return slotDuration;
  }

  @Override
  public int hashCode() {
    return (queueID == null ? 0 : queueID.hashCode())
        ^ (slotStart == null ? 0 : slotStart.hashCode())
        ^ (slotDuration == null ? 0 : slotDuration.hashCode());
  }

  /** A Tripel is equal, if their slotStart time is equal.
   * @param o Object to compare against
   * @return true, if Triples are equal
   */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Triple)) {
      return false;
    }
    return this.getSlotStart().equals(((Triple) o).getSlotStart());
  }

  @Override
  public String toString() {
    return "(" + queueID + "," + slotStart + "," + slotDuration + ")";
  }

  /** A triple is compare based on its slotStart.
   * @param triple to be compared against
   * @return -1,0 or 1 depending on the value of slotStart
   */
  @Override
  public int compareTo(Triple triple) {
    return this.getSlotStart().compareTo(triple.getSlotStart());
  }
}
