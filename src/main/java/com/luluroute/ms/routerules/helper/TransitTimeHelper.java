package com.luluroute.ms.routerules.helper;

import com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo;
import com.logistics.luluroute.redis.shipment.entity.AssignedTransitModes;
import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import com.logistics.luluroute.util.DaysOfWeekUtil;
import com.luluroute.ms.routerules.business.dto.TransitTimeRequest;
import com.luluroute.ms.routerules.business.util.OrderTypes;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

import static com.luluroute.ms.routerules.business.util.Constants.STANDARD_INFO;

@Component
@Slf4j
public class TransitTimeHelper {

	/**
	 * If orderType is Retail, looks for assigned transit mode with ref1 value of RETAIL. Otherwise, look for NULL ref1 value.
	 * It is assumed that if not RETAIL, ECOMM.
	 * @param entityProfile Entity Profile
	 * @param carrierCode Selected Carrier
	 * @param carrierModeCode Selected Carrier Mode Code
	 * @param orderType Order Type
	 * @return AssignedTransitMode match entry from entity profile
	 */
	public AssignedTransitModes getAssignedTransitModes(EntityPayload entityProfile, String carrierCode,
			String carrierModeCode, String orderType) {
		String msg = "AssignedTransitModes.getAssignedTransitModes()";

		for (AssignedTransitModes assignedTransitModes : entityProfile.getAssignedTransitModes()) {
			if (carrierCode.equalsIgnoreCase(assignedTransitModes.getCarrierCode()) && carrierModeCode.equalsIgnoreCase(assignedTransitModes.getModeCode())) {
				if (isRetailAssignedTransitMode(orderType, assignedTransitModes.getRef1())) {
					return assignedTransitModes;
				} else if (isNonRetailAssignedTransitMode(orderType, assignedTransitModes.getRef1())) {
					return assignedTransitModes;
				}
			}
		}
		return null;
	}

	public TransitTimeRequest prepareTransitTimeRequestData(long plannedShipDate, ShipmentInfo shipmentInfo,
			EntityPayload entityProfile, AssignedTransitModes assignedTransitModes, String carrierModeCode) {
		return TransitTimeRequest.builder().orderType(shipmentInfo.getOrderDetails().getOrderType())
				.originEntityCode(shipmentInfo.getShipmentHeader().getOrigin().getEntityCode())
				.destinationEntityCode(shipmentInfo.getShipmentHeader().getDestination().getEntityCode())
				.carrierMode(carrierModeCode)
				.originCountry(shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getCountry())
				.originPostalCode(shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getZipCode())
				.originState(shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getState())
				.plannedShipDate(plannedShipDate)
				.destinationCountry(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getCountry())
				.destinationPostalCode(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getZipCode())
				.destinationState(shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getState())
				.isSaturdayDelivery(isSaturdayDelivery(assignedTransitModes)).timeZone(entityProfile.getTimezone())
				.defaultTransitDays(assignedTransitModes.getDefaultTransitDays())
				.weight(shipmentInfo.getShipmentPieces().get(0).getWeightDetails().getValue())
				.weightUOM(shipmentInfo.getShipmentPieces().get(0).getWeightDetails().getUom())
				.valueOfContent(null != shipmentInfo.getOrderDetails().getDeclaredValueDetails()
						? shipmentInfo.orderDetails.getDeclaredValueDetails().getValue()
						: 0)
				.currency(getCurrencyCode(shipmentInfo)).build();
	}
	
	private String getCurrencyCode(ShipmentInfo shipmentInfo) {
		String currencyCode = "USD";
		if (null != shipmentInfo.getShipmentPieces().get(0).cartonsDetails.get(0).itemValue) {
			currencyCode = shipmentInfo.getShipmentPieces().get(0).cartonsDetails.get(0).itemValue.getCurrency();
		} else if (null != shipmentInfo.orderDetails.getDeclaredValueDetails()) {
			currencyCode = String.valueOf(shipmentInfo.orderDetails.getDeclaredValueDetails().getCurrency());
		}
		return currencyCode;
	}

	public boolean isSaturdayDelivery(AssignedTransitModes assignedTransitModes) {
		EnumSet<DaysOfWeekUtil.DaysOfWeek> deliveryDay = DaysOfWeekUtil
				.fromBitValues(assignedTransitModes.getDeliveryDaysMask());
		return deliveryDay.contains(DaysOfWeekUtil.DaysOfWeek.Saturday)
				|| deliveryDay.contains(DaysOfWeekUtil.DaysOfWeek.Sunday);
	}

	/**
	 * @param shipmentOrderType Shipment Message Order Type
	 * @param transitModeOrderType Current Assigned Transit Mode Order Type
	 * @return true if Shipment Message is Retail, Current assigned transit mode's order type is RETAIL
	 */
	private boolean isRetailAssignedTransitMode(String shipmentOrderType, String transitModeOrderType) {
		return OrderTypes.isRetailOrder(shipmentOrderType)
				&& !StringUtils.isEmpty(transitModeOrderType)
				&& transitModeOrderType.equalsIgnoreCase("RETAIL");
	}

	/**
	 *
	 * @param shipmentOrderType Shipment Message Order Type
	 * @param transitModeOrderType Current Assigned Transit Mode Order Type
	 * @return true if Shipment Message is NOT Retail, Current assigned transit mode's order type is null or empty string
	 */
	private boolean isNonRetailAssignedTransitMode(String shipmentOrderType, String transitModeOrderType) {
		return !OrderTypes.isRetailOrder(shipmentOrderType)
				&& StringUtils.isEmpty(transitModeOrderType);
	}
}
